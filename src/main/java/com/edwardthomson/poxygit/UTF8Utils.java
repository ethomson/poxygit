/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.nio.charset.Charset;

public class UTF8Utils
{
	public static final String UTF8_CHARSET_NAME = "UTF-8";
	public static final Charset UTF8_CHARSET = Charset.forName(UTF8_CHARSET_NAME);

	public static String decode(byte[] bytes)
	{
		return new String(bytes, UTF8_CHARSET);
	}

	public static byte[] encode(String string)
	{
		return string.getBytes(UTF8_CHARSET);
	}
}
