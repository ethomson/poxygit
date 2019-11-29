/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream
{
	@Override
	public void write(int b) throws IOException
	{
	}
}
