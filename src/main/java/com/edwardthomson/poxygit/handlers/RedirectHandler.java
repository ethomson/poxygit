package com.edwardthomson.poxygit.handlers;

import java.net.URI;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.RequestInfo.GitRequestType;

public abstract class RedirectHandler extends RequestHandler
{
	private static final String DEFAULT_REDIRECT_HOST = "https://github.com/";

	private final String repository;
	private final String redirectHost;
	private final GitRequestType gitRequestType;
	private final String service;

	public RedirectHandler(Connection connection, String repository, String service)
	{
		super(connection);

		this.repository = repository;
		this.gitRequestType = GitRequestType.References;
		this.service = service;
		this.redirectHost = connection.getOptions().getRedirectHost() != null ? connection.getOptions().getRedirectHost() : DEFAULT_REDIRECT_HOST;
	}

	public RedirectHandler(Connection connection, String repository, GitRequestType gitRequestType)
	{
		super(connection);

		this.repository = repository;
		this.gitRequestType = gitRequestType;
		this.service = null;
		this.redirectHost = connection.getOptions().getRedirectHost() != null ? connection.getOptions().getRedirectHost() : DEFAULT_REDIRECT_HOST;
	}

	protected String getRedirect() throws Exception
	{
		final URI base = new URI(this.redirectHost);
		final StringBuilder redirect = new StringBuilder();
		redirect.append(base.getScheme());
		redirect.append("://");
		redirect.append(base.getHost());
		
		if (base.getPath() != null)
		{
			redirect.append(base.getPath());
		}

		if (redirect.charAt(redirect.length() - 1) != '/')
		{
			redirect.append('/');
		}
		
		redirect.append(this.repository);

		if (gitRequestType == GitRequestType.References)
		{
			redirect.append("/info/refs?service=git-");
			redirect.append(service);			
		}
		else if (gitRequestType == GitRequestType.UploadPack)
		{
			redirect.append("/git-upload-pack");
		}
		else if (gitRequestType == GitRequestType.ReceivePack)
		{
			redirect.append("/git-receive-pack");
		}
		
		return redirect.toString();
	}
}
