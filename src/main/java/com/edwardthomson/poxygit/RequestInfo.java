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
		BrokenNTLM("broken-ntlm");

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
		UploadPack
	}

	private final RequestType requestType;
	private final GitRequestType gitRequestType;
	private final String repositoryPath;
	private final boolean smart;

	private RequestInfo(RequestType requestType, GitRequestType gitRequestType, String repositoryPath, boolean smart)
	{
		this.requestType = requestType;
		this.gitRequestType = gitRequestType;
		this.repositoryPath = repositoryPath;
		this.smart = smart;
	}

	public RequestType getRequestType()
	{
		return requestType;
	}

	public GitRequestType getGitRequestType()
	{
		return gitRequestType;
	}

	public String getRepositoryPath()
	{
		return repositoryPath;
	}

	public boolean isSmart()
	{
		return smart;
	}

	public static RequestInfo parseRequest(Request request) throws FileNotFoundException
	{
		String[] components = request.getURI().split("\\?", 2);
		String[] path = components[0].split("/", 4);

		if (path.length != 4 || !path[0].equals(""))
		{
			throw new FileNotFoundException();
		}

		RequestType requestType;
		String repository = path[2];

		try
		{
			requestType = RequestType.byName(path[1]);
		}
		catch (IllegalArgumentException e)
		{
			throw new FileNotFoundException();
		}

		if (request.getMethod().equals(Constants.GET_METHOD) && path[3].equals("info/refs"))
		{
			return new RequestInfo(requestType, GitRequestType.References, repository,
					components.length == 2 && components[1].equals("service=git-upload-pack"));
		}

		else if (request.getMethod().equals(Constants.POST_METHOD) && path[3].equals("git-upload-pack"))
		{
			return new RequestInfo(requestType, GitRequestType.UploadPack, repository, true);
		}

		throw new FileNotFoundException();
	}
}
