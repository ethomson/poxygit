/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit.handlers;

import java.io.IOException;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.Response;

public abstract class RequestHandler
{
	protected final Connection connection;

	public RequestHandler(Connection connection)
	{
		this.connection = connection;
	}

	/**
	 * @return <code>true</code> if the request was serviced and another request can
	 *         be processed on this connection, <code>false</code> if the request
	 *         was unsuccessful in a way that should prevent further requests from
	 *         being serviced on this connection
	 */
	public abstract boolean handle(Request request, Response response) throws IOException;

	protected String createSmartLine(String data)
	{
		StringBuilder line = new StringBuilder();

		String len = Integer.toHexString(data.length() + 4);

		for (int i = len.length(); i < 4; i++)
		{
			line.append("0");
		}

		line.append(len);
		line.append(data);

		return line.toString();
	}

	@Override
	public String toString()
	{
		return getClass().getName() + " [" + connection + "]";
	}
}
