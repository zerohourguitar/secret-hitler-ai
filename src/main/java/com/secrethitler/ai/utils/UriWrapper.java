package com.secrethitler.ai.utils;

import java.net.URI;
import java.net.URISyntaxException;

public class UriWrapper {
	private URI uri;
	
	public UriWrapper(final String uriString) throws URISyntaxException {
		uri = new URI(uriString);
	}
	
	public URI getUri() {
		return uri;
	}
}
