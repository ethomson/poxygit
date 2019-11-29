/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.edwardthomson.poxygit.logger.LogLevel;
import com.edwardthomson.poxygit.logger.Logger;

public class IOUtils
{
	private final static Logger logger = Logger.getLogger(IOUtils.class);

	/**
	 * Reads one text line from an {@link InputStream} according to HTTP RFC rules,
	 * consuming the first CR/LF or LF encountered.
	 * 
	 * @return the line read from the input stream, <code>null</code> if
	 *         end-of-stream was encountered before reading any characters
	 * @throws IOException if the end-of-stream was encountered after reading at
	 *                     least one character on the line
	 */
	public static String readLine(final InputStream input) throws IOException
	{
		final byte[] rawLine = readRawLine(input);

		if (rawLine == null)
		{
			return null;
		}

		return UTF8Utils.decode(rawLine);
	}

	private static byte[] readRawLine(final InputStream input) throws IOException
	{
		int read = 0;
		final ByteArrayOutputStream line = new ByteArrayOutputStream(128);

		while (true)
		{
			final int b = input.read();

			if (b == -1)
			{

				if (read == 0)
				{
					return null;
				}

				throw new IOException("End of stream while reading request line");
			}
			else if (b == '\r')
			{
				/*
				 * Ignore. This has the effect of discarding bare CRs (those that do not precede
				 * a LF) from the line.
				 */
			}
			else if (b == '\n')
			{
				break;
			}
			else
			{
				line.write((byte) b);
			}

			read++;
		}

		return line.toByteArray();
	}

	public static List<Header> readHeaders(final InputStream input) throws IOException
	{
		final List<Header> ret = new ArrayList<Header>();

		while (true)
		{
			final String line = IOUtils.readLine(input);

			// A null line means end of stream, which shouldn't happen yet
			if (line == null)
			{
				throw new HTTPException("Connection closed while reading headers");
			}

			// An empty line means end of headers
			if (line.length() == 0)
			{
				break;
			}

			final Header h = new Header(line);
			logger.write(LogLevel.TRACE, h.getName() + ": " + h.getValue());
			ret.add(h);
		}

		return ret;
	}

	public static void close(final Socket socket)
	{

		if (socket == null)
		{
			return;
		}

		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			logger.write(LogLevel.DEBUG, "Error closing socket", e);
		}
	}

	/**
	 * Copies count bytes from input to output. If the count is negative, bytes are
	 * copied until the end of stream.
	 */
	public static void copyStream(final InputStream input, final OutputStream output, long count) throws IOException
	{
		final byte[] buffer = new byte[64 * 1024];

		// Easier to duplicate loops than to unify control behavior
		if (count < 0)
		{
			int read;

			while ((read = input.read(buffer)) != -1)
			{
				output.write(buffer, 0, read);
			}
		}
		else
		{

			while (count > 0)
			{
				// Safe to cast to int because it's less than the buffer size
				int maxToRead = count > buffer.length ? buffer.length : (int) count;

				final int read = input.read(buffer, 0, maxToRead);

				if (read == -1)
				{
					return;
				}

				count -= read;

				output.write(buffer, 0, read);
			}
		}
	}

	public static void copyStreamToChunkedStream(InputStream input, OutputStream output) throws IOException
	{
		byte[] buf = new byte[2048];

		while (true)
		{
			int len = input.read(buf, 0, buf.length);

			if (len == -1)
			{
				break;
			}

			writeChunk(output, buf, len);
		}
	}

	public static void copyChunkedStreamToStream(InputStream input, OutputStream output) throws IOException
	{
		byte[] hexlen = new byte[4];
		byte[] buf = new byte[2048];

		while (true)
		{

			if (input.read(hexlen, 0, 4) < 4)
			{
				throw new IOException("end-of-file reading chunk length");
			}

			int len = Integer.parseInt(UTF8Utils.decode(hexlen), 16);

			while (len > 0)
			{
				int chunklen = Math.min(buf.length, len);

				int readlen = input.read(buf, 0, chunklen);

				if (readlen < 0)
				{
					throw new IOException("end-of-file reading stream");
				}

				output.write(buf, 0, readlen);
				len -= readlen;
			}
		}
	}

	public static void writeChunk(OutputStream stream, String data) throws IOException
	{
		writeChunk(stream, UTF8Utils.encode(data));
	}

	public static void writeChunk(OutputStream stream, byte[] data) throws IOException
	{
		writeChunk(stream, data, data.length);
	}

	public static void writeChunk(OutputStream stream, byte[] data, int len) throws IOException
	{
		stream.write(UTF8Utils.encode(Integer.toHexString(len)));
		stream.write(new byte[] { '\r', '\n' });

		stream.write(data, 0, len);
		stream.write(new byte[] { '\r', '\n' });
		stream.flush();
	}

	public static void writeChunkEnd(OutputStream stream) throws IOException
	{
		stream.write(new byte[] { '0', '\r', '\n', '\r', '\n' });
		stream.flush();
	}
}
