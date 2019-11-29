/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import com.edwardthomson.poxygit.logger.LogLevel;
import com.edwardthomson.poxygit.logger.Logger;

public class SocketListener implements Runnable
{
	private final Logger logger = Logger.getLogger(SocketListener.class);

	private final ServerSocket serverSocket;
	private final ExecutorService executorService;
	private final Options options;

	public SocketListener(ServerSocket serverSocket, ExecutorService executorService, Options options)
	{
		this.serverSocket = serverSocket;
		this.executorService = executorService;
		this.options = options;
	}

	protected ServerSocket getServerSocket()
	{
		return serverSocket;
	}

	protected Options getOptions()
	{
		return options;
	}

	protected Socket accept() throws Exception
	{
		return serverSocket.accept();
	}

	@Override
	public final void run()
	{

		while (true)
		{
			Socket client;

			try
			{
				client = accept();
			}
			catch (Exception e)
			{
				logger.write(LogLevel.FATAL, "Could not accept client socket", e);
				continue;
			}

			executorService.submit(new Connection(client, options, executorService));
		}
	}
}
