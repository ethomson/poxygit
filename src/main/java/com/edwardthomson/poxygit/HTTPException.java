/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

/**
 * An exception thrown when the HTTP protocol is malformed or the request is
 * semantically invalid.
 */
public class HTTPException extends RuntimeException
{
	private static final long serialVersionUID = -9187178406815885682L;

	public HTTPException()
	{
		super();
	}

	public HTTPException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public HTTPException(String message)
	{
		super(message);
	}

	public HTTPException(Throwable cause)
	{
		super(cause);
	}
}
