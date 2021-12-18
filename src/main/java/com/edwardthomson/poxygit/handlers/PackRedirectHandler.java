package com.edwardthomson.poxygit.handlers;

import java.io.IOException;

import com.edwardthomson.poxygit.Connection;
import com.edwardthomson.poxygit.Header;
import com.edwardthomson.poxygit.IOUtils;
import com.edwardthomson.poxygit.NullOutputStream;
import com.edwardthomson.poxygit.Request;
import com.edwardthomson.poxygit.RequestInfo.GitRequestType;
import com.edwardthomson.poxygit.Response;
import com.edwardthomson.poxygit.Status;

public class PackRedirectHandler extends RedirectHandler
{
	public PackRedirectHandler(Connection connection, String repository, GitRequestType gitRequestType)
	{
		super(connection, repository, gitRequestType);
	}

	@Override
	public boolean handle(Request request, Response response) throws IOException
	{
		final String redirect;
		try
		{
			redirect = getRedirect();
		}
		catch (Exception e)
		{
			response.writeError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
			return false;
		}

		IOUtils.copyHttpStreamToStream(request.getHeaders(), request.getInputStream(), new NullOutputStream());
		
		response.writeStatus(Status.FOUND, "Found");
		response.writeHeader(new Header("Location", redirect));
		response.writeHeader(new Header("Content-Length", "0"));
		response.writeHeader(new Header("Connection", "close"));
		response.endHeaders();

		return true;
	}
}
