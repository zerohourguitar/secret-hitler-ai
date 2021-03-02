package com.secrethitler.ai.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UrlWrapper {
	private final URL url;
	
	public UrlWrapper(final String urlString) throws MalformedURLException {
		url = new URL(urlString);
	}
	
	public URLConnection openConnection() throws IOException {
		return url.openConnection();
	}
}
