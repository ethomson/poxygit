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
import java.io.OutputStream;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Header;
import com.edwardthomson.poxygit.IOUtils;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.Response;
import com.edwardthomson.poxygit.Status;
import com.edwardthomson.poxygit.logger.Logger;

/**
 * Handles /info/refs requests.
 */
public class ReferencesHandler extends RequestHandler
{
	private static final Logger logger = Logger.getLogger(ReferencesHandler.class);

	private final String repositoryPath;
	private final String service;

	public ReferencesHandler(Connection connection, String repositoryPath, String service)
	{
		super(connection);

		this.repositoryPath = repositoryPath;
		this.service = service;
	}

	@Override
	public boolean handle(Request request, Response response) throws IOException
	{
		response.writeStatus(Status.OK, "OK");
		response.writeHeader(new Header("Expires", "Fri, 01 Jan 1980 00:00:00 GMT"));
		response.writeHeader(new Header("Pragma", "no-cache"));
		response.writeHeader(new Header("Cache-Control", "no-cache, max-age=0, must-revalidate"));
		response.writeHeader(new Header("Transfer-Encoding", "chunked"));
		response.writeHeader(new Header("Content-Type", String.format("application/x-git-%s-advertisement", service)));
		response.endHeaders();

		OutputStream outputStream = response.getStream();
		IOUtils.writeChunk(outputStream, createSmartLine(String.format("# service=git-%s\n", service)));
		IOUtils.writeChunk(outputStream, "0000");

		Process proc = Runtime.getRuntime()
				.exec(new String[] { "git", service, "--stateless-rpc", "--advertise-refs", repositoryPath });
		IOUtils.copyStreamToChunkedStream(proc.getInputStream(), outputStream);

		return true;
	}
}
