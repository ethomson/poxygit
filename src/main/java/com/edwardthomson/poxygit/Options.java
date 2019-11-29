/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Options
{
	/**
	 * Root for git repositories
	 */
	private volatile String projectRoot = null;

	/**
	 * Local address to bind to.
	 */
	private volatile String localAddress = null;

	/**
	 * Local TCP port to bind to.
	 */
	private volatile int localPort = 8000;

	/**
	 * Local TCP port to bind to (SSL/TLS).
	 */
	private volatile int localSSLPort = 0;

	/**
	 * SSL Keystore file path.
	 */
	private volatile String sslKeystoreFile = null;

	/**
	 * Password for SSL Keystore file.
	 */
	private volatile String sslKeystorePassword = null;

	/**
	 * If a connection to a server or forward proxy takes longer than this many
	 * seconds, it errors with 504 Gateway Timeout.
	 */
	private volatile int connectTimeoutSeconds = 10;

	/**
	 * Set on client-to-proxy and proxy-to-server sockets.
	 * <p>
	 * If the client-to-proxy times out, the server socket is just closed.
	 * <p>
	 * If the proxy-to-server socket times out <em>before</em> any data has been
	 * sent back to the server, the client gets a 504 Gateway Timeout.
	 * <p>
	 * If the proxy-to-server socket times out <em>after</em> some data has been
	 * sent to the client, the client-to-proxy socket is simply closed.
	 */
	private volatile int socketReadTimeoutSeconds = 300;

	/**
	 * Thread pool size for processing all requests.
	 */
	private volatile int maxThreads = 100;

	/**
	 * Only used when {@link #authenticationRequired} is true.
	 *
	 * Map of username/password pairs that are permitted when proxy authentication
	 * is enabled.
	 *
	 * Synchronized on {@link #proxyCredentials}.
	 */
	private final Map<String, String> credentials = new HashMap<String, String>();

	/**
	 * The maximum HTTP header size for requests/responses.
	 */
	private final int maxHeaderSizeBytes = 32 * 1024;

	/**
	 * Time to sleep before returning the status code with the response.
	 */
	private int responseDelayMilliseconds;

	public Options()
	{
	}

	public String getProjectRoot()
	{
		return this.projectRoot;
	}

	public void setProjectRoot(String projectRoot)
	{
		this.projectRoot = projectRoot;
	}

	public String getLocalAddress()
	{
		return this.localAddress;
	}

	public void setLocalAddress(String localAddress)
	{
		this.localAddress = localAddress;
	}

	public int getLocalPort()
	{
		return this.localPort;
	}

	public void setLocalPort(int localPort)
	{
		this.localPort = localPort;
	}

	public int getLocalSSLPort()
	{
		return this.localSSLPort;
	}

	public void setLocalSSLPort(int localSSLPort)
	{
		this.localSSLPort = localSSLPort;
	}

	public String getSSLKeystoreFile()
	{
		return this.sslKeystoreFile;
	}

	public void setSSLKeystoreFile(String sslKeystoreFile)
	{
		this.sslKeystoreFile = sslKeystoreFile;
	}

	public String getSSLKeystorePassword()
	{
		return this.sslKeystorePassword;
	}

	public void setSSLKeystorePassword(String sslKeystorePassword)
	{
		this.sslKeystorePassword = sslKeystorePassword;
	}

	public int getConnectTimeoutSeconds()
	{
		return this.connectTimeoutSeconds;
	}

	public void setConnectTimeoutSeconds(int connectTimeoutSeconds)
	{
		this.connectTimeoutSeconds = connectTimeoutSeconds;
	}

	public int getSocketReadTimeoutSeconds()
	{
		return this.socketReadTimeoutSeconds;
	}

	public void setSocketReadTimeoutSeconds(int socketReadTimeoutSeconds)
	{
		this.socketReadTimeoutSeconds = socketReadTimeoutSeconds;
	}

	public int getResponseDelayMilliseconds()
	{
		return responseDelayMilliseconds;
	}

	public void setResponseDelayMilliseconds(int responseDelayMilliseconds)
	{
		this.responseDelayMilliseconds = responseDelayMilliseconds;
	}

	public int getMaxThreads()
	{
		return this.maxThreads;
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
	}

	public void setCredentials(List<String> credentials)
	{

		synchronized (this.credentials)
		{

			for (String credential : credentials)
			{
				String[] parts = credential.split(":", 2);
				this.credentials.put(parts[0], parts[1]);
			}
		}
	}

	public void addCredentials(String username, String password)
	{

		synchronized (credentials)
		{
			credentials.put(username, password);
		}
	}

	public String getCredentials(String username)
	{

		synchronized (credentials)
		{
			return credentials.get(username);
		}
	}

	public boolean credentialsMatch(String username, String password)
	{

		synchronized (credentials)
		{
			return password.equals(credentials.get(username));
		}
	}

	public int getMaxHeaderSizeBytes()
	{
		return maxHeaderSizeBytes;
	}
}
