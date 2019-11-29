/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

import java.net.InetAddress;

public class Utils
{
	private static String hostname = null;

	public static String getHostname()
	{
		if (hostname == null)
		{
			String h = "";
			int dot;

			try
			{
				h = InetAddress.getLocalHost().getHostName();

				if ((dot = h.indexOf('.')) >= 0)
				{
					h = h.substring(0, dot);
				}
			}
			catch (Exception e)
			{
			}

			if (h.length() == 0)
			{
				h = "localhost";
			}

			hostname = h.toUpperCase();
		}

		return hostname;
	}
}
