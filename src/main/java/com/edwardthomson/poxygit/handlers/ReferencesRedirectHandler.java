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
import java.net.URI;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Header;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.RequestInfo.RequestType;
import com.edwardthomson.poxygit.Response;
import com.edwardthomson.poxygit.Status;
import com.edwardthomson.poxygit.logger.Logger;

/**
 * Redirects /info/req requests to a new server.
 */
public class ReferencesRedirectHandler extends RedirectHandler
{	
	private static final Logger logger = Logger.getLogger(ReferencesRedirectHandler.class);

	public ReferencesRedirectHandler(Connection connection, String repository, String service)
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
		
		response.writeStatus(Status.FOUND, "Found");
		response.writeHeader(new Header("Location", redirect));
		response.writeHeader(new Header("Content-Length", "0"));
		response.writeHeader(new Header("Connection", "close"));
		response.endHeaders();

		return true;
	}
}
