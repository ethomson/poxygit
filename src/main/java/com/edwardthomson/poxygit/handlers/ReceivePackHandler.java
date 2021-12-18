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
import java.util.zip.GZIPInputStream;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Header;
import com.edwardthomson.poxygit.HeaderUtils;
import com.edwardthomson.poxygit.IOUtils;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.Response;
import com.edwardthomson.poxygit.Status;
import com.edwardthomson.poxygit.logger.Logger;

public class ReceivePackHandler extends RequestHandler
{
	private static final Logger logger = Logger.getLogger(UploadPackHandler.class);

	private String repositoryPath;

	public ReceivePackHandler(Connection connection, String repositoryPath)
	{
		super(connection);

		this.repositoryPath = repositoryPath;
	}

	@Override
	public boolean handle(Request request, Response response) throws IOException
	{
		long contentLength = HeaderUtils.getContentLength(request.getHeaders());
		boolean chunked = false;
		boolean gzip = HeaderUtils.isGzip(request.getHeaders());

		if (contentLength < 0)
		{
			chunked = HeaderUtils.isChunked(request.getHeaders());

			if (!chunked)
			{
				response.writeStatus(Status.BAD_REQUEST, "no input specified");
				return false;
			}

			if (gzip)
			{
				response.writeStatus(Status.BAD_REQUEST, "cannot handle gzip chunks");
				return false;
			}
		}

		response.writeStatus(Status.OK, "OK");
		response.writeHeader(new Header("Expires", "Fri, 01 Jan 1980 00:00:00 GMT"));
		response.writeHeader(new Header("Pragma", "no-cache"));
		response.writeHeader(new Header("Cache-Control", "no-cache, max-age=0, must-revalidate"));
		response.writeHeader(new Header("Transfer-Encoding", "chunked"));
		response.writeHeader(new Header("Content-Type", "application/x-git-receive-pack-result"));
		response.endHeaders();

		Process proc = Runtime.getRuntime()
				.exec(new String[] { "git", "receive-pack", "--stateless-rpc", repositoryPath });

		IOUtils.copyHttpStreamToStream(request.getHeaders(), request.getInputStream(), proc.getOutputStream());
		IOUtils.copyStreamToChunkedStream(proc.getInputStream(), response.getStream());

		return true;
	}
}
