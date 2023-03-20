/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.io.IOException;
import java.io.OutputStream;

public class ThrottledOutputStream extends OutputStream
{
	private final OutputStream stream;
	private boolean throttled = false;
	private double bytesPerMillisecond = 0.0;

	public ThrottledOutputStream(final OutputStream stream)
	{
		this.stream = stream;
	}

	public ThrottledOutputStream(final OutputStream stream, double bitsPerSecond)
	{
		this.stream = stream;
		this.throttled = true;
		this.bytesPerMillisecond = (bitsPerSecond / 8) / 1000;
	}

	public void setSpeed(double bitsPerSecond)
	{
		this.throttled = true;
		this.bytesPerMillisecond = (bitsPerSecond / 8) / 1000;
	}

	@Override
	public void flush() throws IOException
	{
		stream.flush();
	}

	@Override
	public void close() throws IOException
	{
		stream.close();
	}

	@Override
	public void write(int b) throws IOException
	{
		this.write(new byte[] { (byte)b }, 0, 1);
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		this.write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		int written = 0;

		if (!throttled)
		{
			stream.write(b, off, len);
			return;
		}

		try
		{
			while (len > 0)
			{
				if (bytesPerMillisecond == 0)
				{
					while (true)
					{
						Thread.sleep(5000);
					}
				}
				else if (bytesPerMillisecond > 1)
				{
					Thread.sleep(1);

					int chunklen = Math.min((int)this.bytesPerMillisecond, len);
					stream.write(b, off + written, chunklen);

					written += chunklen;
					len -= chunklen;
				}
				else {
					Thread.sleep((int)(1 / bytesPerMillisecond));

					stream.write(b, off + written, 1);
					stream.flush();

					written += 1;
					len--;
				}
			}
		}
		catch(InterruptedException e)
		{
			throw new IOException(e);
		}
	}
}
