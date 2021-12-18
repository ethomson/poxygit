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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Constants;
import com.edwardthomson.poxygit.Header;
import com.edwardthomson.poxygit.IOUtils;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.Response;
import com.edwardthomson.poxygit.Status;
import com.edwardthomson.poxygit.logger.Logger;

/**
 * Proxies /info/req to another new server.
 */
public class ReferencesProxyHandler extends RedirectHandler
{	
	private static final Logger logger = Logger.getLogger(ReferencesRedirectHandler.class);

	public ReferencesProxyHandler(Connection connection, String repository, String service)
	{
		super(connection, repository, service);
	}

	@Override
	public boolean handle(Request request, Response response) throws IOException
	{
		final String redirect;
		try
		{
			redirect = getRedirect();
		}
		catch (Exception e)
		{
			response.writeError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			return false;
		}

		final URL url = new URL(redirect);
		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		final InputStream referencesStream = connection.getInputStream();
		final OutputStream responseStream = response.getStream();
		
		if (connection.getResponseCode() != 200)
		{
			response.writeError(500, String.format("Unexpected response code: %d", connection.getResponseCode()));
			return false;
		}

		response.writeStatus(connection.getResponseCode());

		String header;
		for (int i = 1; (header = connection.getHeaderFieldKey(i)) != null; i++)
		{
			if (header.equals("Transfer-Encoding"))
			{
				continue;
			}

			response.writeHeader(new Header(header, connection.getHeaderField(header)));			
		}
				
		response.writeHeader(new Header("Via", Constants.PROGRAM_NAME));
		response.writeHeader(new Header("Transfer-Encoding", "chunked"));
		response.writeHeader(new Header("Connection", "close"));
		
		response.endHeaders();
		
		IOUtils.copyStreamToChunkedStream(referencesStream, responseStream);

		return true;
	}
}
