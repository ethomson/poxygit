/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.edwardthomson.poxygit.logger.LogLevel;
import com.edwardthomson.poxygit.logger.Logger;

/**
 * Writes an HTTP response. Offers methods for writing lines which terminate
 * them with a CRLF. Does not buffer any part of the response, and methods to
 * set status, write headers, then write response body must be called in order.
 * <p>
 * All output is buffered, so call {@link #flush()} to ensure it's written.
 */
public class Response
{
	private final static Logger logger = Logger.getLogger(Response.class);

	private final ThrottledOutputStream throttler;
	private final CountingOutputStream out;

	private String version;
	private long contentLength = -1;
	private List<Header> headers = new ArrayList<Header>();
	private boolean shouldClose = false;

	public Response(final OutputStream out, final String version)
	{
		this.throttler = new ThrottledOutputStream(out);
		this.out = new CountingOutputStream(new BufferedOutputStream(throttler));
		this.version = version;
	}

	public void setThrottledSpeed(double bps)
	{
		this.throttler.setSpeed(bps);
	}

	public void setClose(boolean shouldClose)
	{
		this.shouldClose = shouldClose;
	}

	public boolean shouldClose()
	{
		return this.shouldClose;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	/**
	 * @return the value of the {@link Constants#CONTENT_LENGTH_HEADER} header if it
	 *         was written to the response and could be parsed as a {@link Long},
	 *         otherwise -1
	 */
	public long getContentLengthHeaderValue()
	{
		return contentLength;
	}

	public long getActualResponseBodyLength()
	{
		return out.getCount();
	}

	public void setHeaders(List<Header> headers)
	{
		this.headers = headers;
	}

	public List<Header> getHeaders()
	{
		return headers;
	}

	public void writeContinue() throws IOException
	{
		writeStatus(100, "Continue");
		endHeaders();

		flush();
	}

	/**
	 * Sends a complete error response to the user and flushes the output.
	 * <p>
	 * Can only be called before writing any other data to the response and no more
	 * data can be written after this is called.
	 */
	public void writeError(int status, Throwable t) throws IOException
	{
		writeError(status, t.toString());
	}

	/**
	 * Sends a complete error response to the user and flushes the output.
	 * <p>
	 * Can only be called before writing any other data to the response and no more
	 * data can be written after this is called.
	 */
	public void writeError(int status, String details) throws IOException
	{
		String statusName = Status.NAMES.get(status);
		String message = MessageFormat.format("<html><head><title>Error</title></head><body><p>{0}</p></body></html>\n",
				details);

		writeStatus(status, statusName != null ? statusName : "Error");
		writeHeader(new Header(Constants.CONTENT_TYPE_HEADER, Constants.CONTENT_TYPE_TEXT_HTML));
		writeHeader(new Header(Constants.CONTENT_LENGTH_HEADER, Integer.toString(message.length())));
		endHeaders();

		out.write(UTF8Utils.encode(message));

		flush();
	}

	public OutputStream getStream()
	{
		return out;
	}

	public void writeStatus(int status) throws IOException
	{
		writeStatus(status, null);
	}

	public void writeStatus(int status, String message) throws IOException
	{
		writeStatus(status, message, version);
	}

	public void writeStatus(int status, String message, String httpVersion) throws IOException
	{
		final String s = MessageFormat.format("{0} {1} {2}", httpVersion, Integer.toString(status),
				message != null ? message : Status.NAMES.get(status));

		logger.write(LogLevel.DEBUG, s);
		writeLine(s);
	}

	public void writeHeaders(Iterable<Header> headers) throws IOException
	{

		for (Header h : headers)
		{
			writeHeader(h);
		}
	}

	public void writeHeader(Header h) throws IOException
	{

		if (h == null)
		{
			return;
		}

		// Sanity test for the ConnectionHandler
		if (h.getName().equalsIgnoreCase(Constants.CONTENT_LENGTH_HEADER))
		{

			try
			{
				contentLength = Long.parseLong(h.getValue());
			}
			catch (NumberFormatException e)
			{
				logger.write(LogLevel.WARNING, "Couldn't parse content length " + h.getValue() + " as Long", e);
			}
		}

		writeLine(h.toString());
	}

	public void endHeaders() throws IOException
	{
		writeLine("");

		// Reset so we can measure response content size
		out.resetCount();
	}

	/**
	 * Writes a line and a CRLF.
	 * <p>
	 * Ensure the content is written by calling {@link #flush()} before using the
	 * {@link OutputStream} from {@link #getStream()}.
	 */
	private void writeLine(String line) throws IOException
	{

		if (line == null)
		{
			return;
		}

		out.write(UTF8Utils.encode(line + "\r\n"));
	}

	public void flush() throws IOException
	{
		out.flush();
	}
}
