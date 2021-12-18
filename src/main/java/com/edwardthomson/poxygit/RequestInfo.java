/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.io.FileNotFoundException;

public class RequestInfo
{
	public enum RequestType
	{
		Anonymous("anonymous"),
		Basic("basic"),
		NTLM("ntlm"),
		BrokenNTLM("broken-ntlm"),
        InitialRedirect("initial-redirect"),
        SubsequentRedirect("subsequent-redirect");

		private String name;

		private RequestType(String name)
		{
			this.name = name;
		}

		public static RequestType byName(String name)
		{

			for (RequestType type : RequestType.values())
			{

				if (type.name.equalsIgnoreCase(name))
				{
					return type;
				}
			}

			throw new IllegalArgumentException("Request type not found");
		}
	}

	public enum GitRequestType
	{
		References,
		UploadPack,
		ReceivePack
	}

	private final RequestType requestType;
	private final GitRequestType gitRequestType;
	private final String repositoryPath;
	private final String service;

	private RequestInfo(RequestType requestType, GitRequestType gitRequestType, String repositoryPath)
	{
		this(requestType, gitRequestType, null, repositoryPath);
	}

	private RequestInfo(RequestType requestType, GitRequestType gitRequestType, String service, String repositoryPath)
	{
		this.requestType = requestType;
		this.gitRequestType = gitRequestType;
		this.service = service;
		this.repositoryPath = repositoryPath;
	}

	public RequestType getRequestType()
	{
		return requestType;
	}

	public GitRequestType getGitRequestType()
	{
		return gitRequestType;
	}

	public String getService()
	{
		return service;
	}

	public String getRepositoryPath()
	{
		return repositoryPath;
	}
	
	private static String joinRepositoryPath(String[] path, int start, int end)
	{
		final StringBuilder result = new StringBuilder();

		for (int i = start; i < end; i++)
		{
			if (i > start)
			{
				result.append('/');
			}
			
			result.append(path[i]);
		}
		
		return result.toString();
	}

	public static RequestInfo parseRequest(Request request) throws FileNotFoundException
	{
		String[] components = request.getURI().split("\\?", 2);
		String[] path = components[0].split("/");

		if (path.length < 4 || !path[0].equals(""))
		{
			throw new FileNotFoundException();
		}

		final RequestType requestType;
		final GitRequestType gitRequestType;
		final String repository;

		if (path[path.length - 2].equals("info") && path[path.length - 1].equals("refs"))
		{
			gitRequestType = GitRequestType.References;
			repository = joinRepositoryPath(path, 2, path.length - 2);
		}
		else if (path[path.length - 1].equals("git-upload-pack"))
		{
			gitRequestType = GitRequestType.UploadPack;
			repository = joinRepositoryPath(path, 2, path.length - 1);			
		}
		else if (path[path.length - 1].equals("git-receive-pack"))
		{
			gitRequestType = GitRequestType.ReceivePack;
			repository = joinRepositoryPath(path, 2, path.length - 1);
		}
		else
		{
			throw new FileNotFoundException();			
		}

		try
		{
			requestType = RequestType.byName(path[1]);
		}
		catch (IllegalArgumentException e)
		{
			throw new FileNotFoundException();
		}

		if (request.getMethod().equals(Constants.GET_METHOD) && gitRequestType == GitRequestType.References)
		{
			if (components[1].equals("service=git-upload-pack"))
			{
				return new RequestInfo(requestType, GitRequestType.References, "upload-pack", repository);
			}
			else if (components[1].equals("service=git-receive-pack"))
			{
				return new RequestInfo(requestType, GitRequestType.References, "receive-pack", repository);
			}
			else
			{
				return new RequestInfo(requestType, GitRequestType.References, repository);
			}
		}

		else if (request.getMethod().equals(Constants.POST_METHOD) && gitRequestType == GitRequestType.UploadPack)
		{
			return new RequestInfo(requestType, GitRequestType.UploadPack, repository);
		}

		else if (request.getMethod().equals(Constants.POST_METHOD) && gitRequestType == GitRequestType.ReceivePack)
		{
			return new RequestInfo(requestType, GitRequestType.ReceivePack, repository);
		}

		throw new FileNotFoundException();
	}
}
