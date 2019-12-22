/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.edwardthomson.poxygit.RequestInfo.GitRequestType;
import com.edwardthomson.poxygit.RequestInfo.RequestType;
import com.edwardthomson.poxygit.handlers.ReferencesHandler;
import com.edwardthomson.poxygit.handlers.RequestHandler;
import com.edwardthomson.poxygit.handlers.UploadPackHandler;
import com.edwardthomson.poxygit.logger.LogLevel;
import com.edwardthomson.poxygit.logger.Logger;

/**
 * A connection corresponds to one client-to-proxy TCP socket, which is
 * long-lived (handles multiple HTTP requests) when HTTP keep-alive is enabled,
 * short lived otherwise.
 * <p>
 * This is a {@link Runnable} and is always run in its own thread. It runs until
 * the client-to-proxy socket closes, or until the proxy-to-server socket
 * closes, or until some fatal error causes both sides to close.
 */
public class Connection implements Runnable
{
	private final static Logger logger = Logger.getLogger(Connection.class);

	private final Socket client;
	private final Options options;
	private final ExecutorService executorService;

	/*
	 * Session auth mechanisms like NTLM will authenticate the entire keep-alive
	 * session.
	 */
	private boolean authenticated = false;
	private NTLMMessage.Type2Message ntlmChallenge;

	private enum RequestStatus
	{
		Stop,
		Retry,
		SendContinue,
		OK
	}

	private class RequestRoute
	{
		private final RequestStatus status;
		private final RequestHandler handler;

		private RequestRoute(RequestStatus status)
		{
			this.status = status;
			this.handler = null;
		}

		private RequestRoute(RequestHandler handler)
		{
			this.status = RequestStatus.OK;
			this.handler = handler;
		}

		public RequestStatus getStatus()
		{
			return status;
		}

		public RequestHandler getHandler()
		{
			return handler;
		}
	}

	public Connection(final Socket client, final Options options, final ExecutorService executorService)
	{
		this.client = client;
		this.options = options;
		this.executorService = executorService;
	}

	public Options getOptions()
	{
		return options;
	}

	public ExecutorService getExecutorService()
	{
		return executorService;
	}

	public Socket getClient()
	{
		return client;
	}

	@Override
	public void run()
	{
		long requestCount = 0;
		boolean connectionHeaderRead = false;
		boolean keepAlive = true;

		String oldName = Thread.currentThread().getName();
		Thread.currentThread().setName("Connection-" + client.getRemoteSocketAddress());

		try
		{
			initializeclient();

			final InputStream in = client.getInputStream();
			final OutputStream out = client.getOutputStream();

			while (keepAlive)
			{
				// Allocate a response with a default version so we can respond
				// to request protocol errors

				final Response response = new Response(out, Constants.VERSION_10);

				// Read the request

				final Request request = new Request(in);

				try
				{

					if (!request.read())
					{

						/*
						 * Socket closed before reading any part of request, which is a valid way to
						 * close a kept-alive connection that has already done at least one request, but
						 * is invalid otherwise.
						 */
						if (!keepAlive || requestCount == 0)
						{
							logger.write(LogLevel.WARNING,
									"Connection closed before request could be read on socket " + client);
						}
						break;
					}

					requestCount++;

					final Header connectionHeader = findHeader(Constants.CONNECTION_HEADER, request.getHeaders());

					if (connectionHeader != null)
					{
						keepAlive = connectionHeader.getValue().equalsIgnoreCase(Constants.CONNECTION_KEEP_ALIVE);
						connectionHeaderRead = true;
					}
					else if (!connectionHeaderRead)
					{
						keepAlive = request.getVersion().equals(Constants.VERSION_11);
					}
				}
				catch (HTTPException e)
				{
					// Protocol error or similar
					response.writeError(Status.BAD_REQUEST, e);
					break;
				}
				catch (SocketException e)
				{
					// Socket problem so don't try to write an error response
					logger.write(LogLevel.DEBUG, "SocketException", e);
					break;
				}
				catch (IOException e)
				{
					// A non-protocol error, but still don't try to write
					// an error response
					logger.write(LogLevel.DEBUG, "Non protocol exception doing socket IO", e);
					break;
				}

				// Upgrade the response to use the version the client gave us
				response.setVersion(request.getVersion());

				final RequestRoute routing = route(request, response);

				if (routing.getStatus() == RequestStatus.Stop)
				{
					break;
				}
				else if (routing.getStatus() == RequestStatus.Retry)
				{
					continue;
				}

				if (HeaderUtils.isExpectContinue(request.getHeaders()))
				{
					response.writeContinue();
				}

				final RequestHandler handler = routing.getHandler();

				/*
				 * After here we can't write an error response because some bytes may have
				 * already been sent by the handler. Handlers mostly handle their own errors.
				 */

				if (!handler.handle(request, response))
				{
					/*
					 * The handler was unsuccessful and we should close this connection.
					 */
					logger.write(LogLevel.DEBUG, "Handler " + handler + " was unsuccessful, closing connection");

					// Best effort flush
					try
					{
						response.flush();
					}
					catch (IOException e)
					{
						// Ignore
					}

					break;
				}

				// Ensure everything was written
				response.flush();

				/*
				 * Make sure we wrote the same number of bytes the header declared. If we wrote
				 * too few the client will may wait a long time to get more; if we wrote too
				 * many the client may close the connection on us.
				 *
				 * Skip this for a HEAD request, because we write no body in that case.
				 */
				if (!request.getMethod().equalsIgnoreCase(Constants.HEAD_METHOD) &&
						response.getContentLengthHeaderValue() != -1 &&
						response.getContentLengthHeaderValue() != response.getActualResponseBodyLength())
				{
					logger.write(LogLevel.WARNING,
							MessageFormat.format("Header Content-Length {0} != {1} actually written bytes",
									response.getContentLengthHeaderValue(), response.getActualResponseBodyLength()));

					break;
				}

				if (HeaderUtils.isConnectionClose(response.getHeaders()))
				{
					keepAlive = false;
				}
			}
		}
		catch (SocketTimeoutException e)
		{
			logger.write(LogLevel.DEBUG, "Read timeout on " + client);
		}
		catch (IOException e)
		{
			logger.write(LogLevel.DEBUG, "IOException on socket " + client, e);
		}
		catch (Exception e)
		{
			logger.write(LogLevel.WARNING, "Unhandled exception on socket " + client, e);
		}
		catch (Throwable t)
		{
			logger.write(LogLevel.FATAL, "Unhandled throwable on socket " + client, t);
		}
		finally
		{
			IOUtils.close(client);
			Thread.currentThread().setName(oldName);
		}
	}

	private RequestRoute route(Request request, Response response) throws Exception
	{
		RequestInfo requestInfo;

		try
		{
			requestInfo = RequestInfo.parseRequest(request);
		}
		catch (FileNotFoundException e)
		{
			response.writeError(Status.NOT_FOUND, "Path not found");
			return new RequestRoute(RequestStatus.Stop);
		}

		String repository = requestInfo.getRepositoryPath();

		if (repository.contains("/") || repository.contains("\\") || repository.equals(".") || repository.equals(".."))
		{
			response.writeError(Status.NOT_FOUND, "Path not found");
			return new RequestRoute(RequestStatus.Stop);
		}

		String repositoryPath = options.getProjectRoot() + "/" + repository;

		if (requestInfo.getRequestType() == RequestType.Basic || requestInfo.getRequestType() == RequestType.NTLM ||
				requestInfo.getRequestType() == RequestType.BrokenNTLM)
		{

			if (!authenticate(requestInfo, request, response))
			{
				return new RequestRoute(RequestStatus.Retry);
			}
		}

		if (requestInfo.getGitRequestType() == GitRequestType.References && !requestInfo.isSmart())
		{
			response.writeError(Status.BAD_REQUEST, "Dumb HTTP is not supported");
			return null;
		}
		else if (requestInfo.getGitRequestType() == GitRequestType.References)
		{
			return new RequestRoute(new ReferencesHandler(this, repositoryPath));
		}
		else if (requestInfo.getGitRequestType() == GitRequestType.UploadPack)
		{
			return new RequestRoute(new UploadPackHandler(this, repositoryPath));
		}

		response.writeError(Status.INTERNAL_SERVER_ERROR, "Unhandled " + request.getMethod() + " request");

		return new RequestRoute(RequestStatus.Stop);
	}

	private boolean authenticate(RequestInfo requestInfo, Request request, Response response) throws Exception
	{
		final Header authentication = findHeader(Constants.AUTHORIZATION_HEADER, request.getHeaders());
		final List<Header> responseHeaders = response.getHeaders();
		String challengeMessage = null;

		if (authentication != null)
		{
			logger.write(LogLevel.TRACE, "Received authentication header: " + authentication.getValue());
		}

		if (authenticated)
		{
			logger.write(LogLevel.DEBUG, "Connection authentication; continuing");
			return true;
		}
		else if ((requestInfo.getRequestType() == RequestType.NTLM ||
				requestInfo.getRequestType() == RequestType.BrokenNTLM) && authentication != null &&
				authentication.getValue().startsWith("NTLM "))
		{
			byte[] data = Base64.getDecoder().decode(authentication.getValue().substring(5));
			NTLMMessage message = NTLMMessage.parse(data);

			if (message.getType() == 1)
			{
				ntlmChallenge = NTLM.createChallenge((NTLMMessage.Type1Message) message);
				challengeMessage = Base64.getEncoder().encodeToString(ntlmChallenge.createMessage());
			}
			else if (ntlmChallenge != null && message.getType() == 3)
			{
				NTLMMessage.Type3Message responseMessage = (NTLMMessage.Type3Message) message;
				String username = responseMessage.getUsername();
				String password = options.getCredentials(username);

				if (password != null && NTLM.verifyResponse(username, null, password, ntlmChallenge, responseMessage))
				{
					logger.write(LogLevel.DEBUG, "NTLM authentication accepted");

					authenticated = (requestInfo.getRequestType() != RequestType.BrokenNTLM);
					return true;
				}

				logger.write(LogLevel.DEBUG, "Authentication failed in NTLM response");
				ntlmChallenge = null;
			}
			else
			{
				logger.write(LogLevel.DEBUG, "Invalid NTLM message received");
				ntlmChallenge = null;
			}
		}
		else if (requestInfo.getRequestType() == RequestType.Basic && authentication != null &&
				authentication.getValue().startsWith("Basic "))
		{
			String value = new String(Base64.getDecoder().decode(authentication.getValue().substring(6)),
					Charset.forName("UTF-8"));
			String[] credentials = value.split(":", 2);

			if (options.credentialsMatch(credentials[0], credentials[1]))
			{
				logger.write(LogLevel.DEBUG, "Basic authentication accepted");
				return true;
			}

			logger.write(LogLevel.DEBUG, "Authentication failed in Basic response");
		}

		if (request.getMethod().equals(Constants.POST_METHOD) && !HeaderUtils.isExpectContinue(request.getHeaders()) &&
				!readRequestBuffer(request, response))
		{
			return false;
		}

		response.writeStatus(Status.AUTHENTICATION_REQUIRED, "Authentication Required");

		if (requestInfo.getRequestType() == RequestType.NTLM || requestInfo.getRequestType() == RequestType.BrokenNTLM)
		{

			if (challengeMessage != null)
			{
				logger.write(LogLevel.DEBUG, "Sending NTLM challenge");
				logger.write(LogLevel.TRACE, "Challenge is: NTLM " + challengeMessage);

				responseHeaders.add(new Header(Constants.AUTHENTICATE_HEADER, "NTLM " + challengeMessage));
			}
			else
			{
				logger.write(LogLevel.DEBUG, "Sending NTLM authentication request");
				responseHeaders.add(new Header(Constants.AUTHENTICATE_HEADER, "NTLM"));
			}
		}
		else if (requestInfo.getRequestType() == RequestType.Basic)
		{
			logger.write(LogLevel.DEBUG, "Sending Basic authentication request");
			responseHeaders.add(new Header(Constants.AUTHENTICATE_HEADER, "Basic realm=\"Git\""));
		}

		String responseHtml = "<html><head><title>Authentication Required</title></head><body><p>Authentication Required</p></body></html>";
		byte[] responseBytes = responseHtml.getBytes(StandardCharsets.US_ASCII);

		responseHeaders.add(new Header(Constants.CONTENT_LENGTH_HEADER, Integer.toString(responseBytes.length)));
		responseHeaders.add(
				new Header(Constants.CONTENT_TYPE_HEADER, Constants.CONTENT_TYPE_TEXT_HTML + "; charset=iso-8859-1"));

		response.writeHeaders(responseHeaders);
		response.endHeaders();

		response.getStream().write(responseBytes);
		response.flush();
		return false;
	}

	private boolean readRequestBuffer(Request request, Response response) throws IOException
	{
		long contentLength = HeaderUtils.getContentLength(request.getHeaders());
		boolean chunked = false;
		OutputStream outputStream = new NullOutputStream();

		if (contentLength < 0)
		{
			chunked = HeaderUtils.isChunked(request.getHeaders());

			if (!chunked)
			{
				response.writeStatus(Status.BAD_REQUEST, "no input specified");
				outputStream.close();
				return false;
			}

			IOUtils.copyChunkedStreamToStream(request.getInputStream(), outputStream);
		}
		else
		{
			IOUtils.copyStream(request.getInputStream(), outputStream, contentLength);
		}

		outputStream.close();
		return true;
	}

	private void initializeclient() throws SocketException
	{
		client.setTcpNoDelay(true);
		client.setSoTimeout(options.getSocketReadTimeoutSeconds() * 1000);
	}

	private Header findHeader(final String name, final List<Header> list)
	{

		for (Header h : list)
		{

			if (name.equals(h.getName()))
			{
				return h;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return client.toString();
	}
}
