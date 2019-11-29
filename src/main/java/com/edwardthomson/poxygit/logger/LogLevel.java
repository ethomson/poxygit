/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit.logger;

public enum LogLevel
{
	NONE(0),
	FATAL(1),
	ERROR(2),
	WARNING(3),
	INFO(4),
	DEBUG(5),
	TRACE(6);

	private int value;

	private LogLevel(int value)
	{
		this.value = value;
	}

	int getValue()
	{
		return value;
	}
}
