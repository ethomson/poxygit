/*
 * PoxyGit: a simple HTTP Git server for testing.
 *
 * Copyright (c) Edward Thomson.
 * Copyright (c) Microsoft Corporation.
 *
 * All rights reserved.
 */

package com.edwardthomson.poxygit;

public interface Constants
{
	// Program Information

	public static final String PROGRAM_NAME = "poxygit";
	public static final String PROGRAM_VERSION = "0.1.0";

	// Versions

	public static final String VERSION_11 = "HTTP/1.1";
	public static final String VERSION_10 = "HTTP/1.0";

	// Methods

	public static final String GET_METHOD = "GET";
	public static final String POST_METHOD = "POST";
	public static final String HEAD_METHOD = "HEAD";
	public static final String CONNECT_METHOD = "CONNECT";

	// Headers

	public static final String CONNECTION_HEADER = "Connection";
	public static final String CONNECTION_KEEP_ALIVE = "Keep-Alive";
	public static final String CONNECTION_CLOSE = "Close";

	public static final String PROXY_CONNECTION_HEADER = "Proxy-Connection";

	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";

	public static final String CONTENT_LENGTH_HEADER = "Content-Length";

	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String CONTENT_TYPE_TEXT_HTML = "text/html";

	public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
	public static final String CONTENT_ENCODING_GZIP = "gzip";

	public static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";
	public static final String TRANSFER_ENCODING_CHUNKED = "chunked";
	public static final String TRANSFER_ENCODING_IDENTITY = "identity";

}
