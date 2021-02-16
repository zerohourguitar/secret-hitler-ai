package com.secrethitler.ai.websockets;

import java.net.URI;

import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

public abstract class WebsocketClientEndpoint {
	protected Session userSession = null;

	protected void setupWebsocketClientEndpoint(URI endpointURI) {
	    try {
	        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	        container.connectToServer(this, endpointURI);
	    } catch (Exception e) {
	        throw new IllegalStateException(e);
	    }
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
	public void sendMessage(String message) {
	    this.userSession.getAsyncRemote().sendText(message);
	}
	
	/**
	 * Message handler.
	 *
	 * @author Jiji_Sasidharan
	 */
	public static interface MessageHandler {
	
	    public void handleMessage(String message);
	}
}
