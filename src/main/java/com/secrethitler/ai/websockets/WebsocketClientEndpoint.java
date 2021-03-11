package com.secrethitler.ai.websockets;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.secrethitler.ai.utils.UriWrapper;

public abstract class WebsocketClientEndpoint {
	private static final Logger LOGGER = Logger.getLogger(WebsocketClientEndpoint.class.getName());
	protected static final Function<String, UriWrapper> URI_BUILDER_FUNCTION = uriString -> {
		try {
			return new UriWrapper(uriString);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	};
	
	protected static final BiConsumer<WebsocketClientEndpoint, UriWrapper> URI_CONNECTION_CONSUMER = (endpoint, uri) -> {
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(endpoint, uri.getUri());
		} catch (Exception e) {
	        throw new IllegalStateException(e);
	    }
	};
	
	public abstract static class Builder {
		protected Function<String, UriWrapper> uriBuilderFunction = URI_BUILDER_FUNCTION;
		protected BiConsumer<WebsocketClientEndpoint, UriWrapper> uriConnectionConsumer = URI_CONNECTION_CONSUMER;
		
		protected Builder() {
			super();
		}
		
		public abstract WebsocketClientEndpoint build() throws URISyntaxException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;
		
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}
	}
	
	protected Session userSession = null;
	private final BiConsumer<WebsocketClientEndpoint, UriWrapper> uriConnectionConsumer;
	
	protected WebsocketClientEndpoint(Builder builder) {
		this.uriConnectionConsumer = builder.uriConnectionConsumer;
	}

	protected void setupWebsocketClientEndpoint(final UriWrapper endpointUri) {
		uriConnectionConsumer.accept(this, endpointUri);
	}
	
	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession the userSession which is opened.
	 */
	@OnOpen
	public void onOpen(Session userSession) {
	    this.userSession = userSession;
	}
	
	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession the userSession which is getting closed.
	 * @param reason the reason for connection close
	 */
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
	    this.userSession = null;
	}
	
	/**
	 * Callback hook for Message Events. This method will be invoked when a client send a message.
	 *
	 * @param message The text message
	 */
	@OnMessage
	public abstract void onMessage(String message);
	
	/**
	 * Send a message.
	 *
	 * @param message
	 */
	public void sendMessage(final String message) {
		LOGGER.fine(() -> String.format("Sending message: %s", message)); 
	    this.userSession.getAsyncRemote().sendText(message);
	}
}
