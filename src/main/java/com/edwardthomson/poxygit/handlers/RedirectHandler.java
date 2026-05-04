package com.edwardthomson.poxygit.handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Constants;
import com.edwardthomson.poxygit.Header;
import com.edwardthomson.poxygit.HeaderUtils;
import com.edwardthomson.poxygit.IOUtils;
import com.edwardthomson.poxygit.NullOutputStream;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.RequestInfo.GitRequestType;
import com.edwardthomson.poxygit.Response;
import com.edwardthomson.poxygit.Status;
import com.edwardthomson.poxygit.UTF8Utils;

public class RedirectHandler extends RequestHandler {
	private final String repository;
	private final GitRequestType gitRequestType;
	private final String service;
	private final RedirectHandlerOptions redirectOptions;
	
	public enum RedirectHandlerResponseLength {
		None, Standard, Padded;
		
		public static RedirectHandlerResponseLength parse(String message) throws Exception
		{
			if (message == null)
			{
				return null;
			}

			if (message.compareTo("none") == 0)
			{
				return RedirectHandlerResponseLength.None;
			}
			else if (message.compareTo("standard") == 0)
			{
				return RedirectHandlerResponseLength.Standard;
			}
			else if (message.compareTo("padded") == 0)
			{
				return RedirectHandlerResponseLength.Padded;
			}
			else
			{
				throw new Exception("Invalid response length");
			}
		}
	};

	public static class RedirectHandlerOptions
	{
		public URI uri;
		public RedirectHandlerResponseLength responseLength = RedirectHandlerResponseLength.Standard;
	}

	public RedirectHandler(Connection connection, String repository, String service, final RedirectHandlerOptions redirectOptions) {
		super(connection);

		this.repository = repository;
		this.gitRequestType = GitRequestType.References;
		this.service = service;
		this.redirectOptions = redirectOptions;
	}

	public RedirectHandler(Connection connection, String repository, GitRequestType gitRequestType, final RedirectHandlerOptions redirectOptions) {
		super(connection);

		this.repository = repository;
		this.gitRequestType = gitRequestType;
		this.service = null;
		this.redirectOptions = redirectOptions;
	}
	
	private URI getLocalUri(Request request) throws URISyntaxException
	{
		final String scheme = "http";
		final String host = HeaderUtils.getHeader(request.getHeaders(), Constants.HOST_HEADER);
		final String path = request.getURI();

		return new URI(scheme + "://" + host + path);
	}

	protected String getRedirect(Request request) throws Exception
	{
		final URI target = redirectOptions.uri != null ? redirectOptions.uri : new URI("https://github.com/");
		final URI base = getLocalUri(request).resolve(target);

		final StringBuilder redirect = new StringBuilder();
		
		redirect.append(base.toString());

		if (redirect.charAt(redirect.length() - 1) != '/') {
			redirect.append('/');
		}

		redirect.append(this.repository);

		if (gitRequestType == GitRequestType.References) {
			redirect.append("/info/refs?service=git-");
			redirect.append(service);
		} else if (gitRequestType == GitRequestType.UploadPack) {
			redirect.append("/git-upload-pack");
		} else if (gitRequestType == GitRequestType.ReceivePack) {
			redirect.append("/git-receive-pack");
		}

		return redirect.toString();
	}

	@Override
	public boolean handle(Request request, Response response) throws IOException {
		final String redirect;
		try {
			redirect = getRedirect(request);
		} catch (Exception e) {
			response.writeError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			return false;
		}

		if (request.getMethod().equals("POST")) {
			IOUtils.copyHttpStreamToStream(request.getHeaders(), request.getInputStream(), new NullOutputStream());
		}

		final String message = "This document has moved to " + redirect + "\n";
		final byte[] rawMessage;

		if (redirectOptions.responseLength == RedirectHandlerResponseLength.None)
		{
			rawMessage = new byte[0];
		}
		else if (redirectOptions.responseLength == RedirectHandlerResponseLength.Padded)
		{
			/* Pad larger than the size of a TLS packet to ensure that we spill over. */
			String padded = "";

			for (int i = 0; i < (16 * 1024) + 1; i++)
			{
				padded = padded + ".";
			}

			rawMessage = (message + padded).getBytes(UTF8Utils.UTF8_CHARSET);
		}
		else
		{
			rawMessage = message.getBytes(UTF8Utils.UTF8_CHARSET);
		}

		response.writeStatus(Status.FOUND, "Found");
		response.writeHeader(new Header("Location", redirect));
		response.writeHeader(new Header("Content-Type", "text/html; charset=utf-8"));
		response.writeHeader(new Header("Content-Length", Integer.toString(rawMessage.length)));
		response.endHeaders();

		response.getStream().write(rawMessage);

		return true;
	}
}
