package com.secrethitler.ai.websockets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.secrethitler.ai.utils.UriWrapper;
import com.secrethitler.ai.websockets.WebsocketClientEndpoint.Builder;

public abstract class WebsocketClientEndpointTest {
	@Mock
	protected Session userSession;
	
	@Mock
	protected Async async;
	
	private WebsocketClientEndpoint endpoint;
	
	@Before
	public void setUp() throws URISyntaxException, DeploymentException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Logger.getLogger(WebsocketClientEndpoint.class.getName()).setLevel(Level.FINE);
		endpoint = getEndpoint();
		when(userSession.getAsyncRemote()).thenReturn(async);
	}

	@Test
	public void testOnOpen() {
		endpoint.onOpen(userSession);
		
		assertEquals(userSession, endpoint.userSession);
	}
	
	@Test
	public void testOnClose() {
		endpoint.onClose(userSession, null);
		
		assertNull(endpoint.userSession);
	}
	
	@Test
	public void testSendMessage() {
		endpoint.userSession = userSession;
		
		endpoint.sendMessage("Test message");
		
		verify(userSession).getAsyncRemote();
		verify(async).sendText("Test message");
	}
	
	@Test
	public void testBuilderHashCode() {
		WebsocketClientEndpoint.Builder builder = getBuilder();
		assertNotEquals(0, builder.hashCode());
	}
	
	@Test
	public void testBuilderToString() {
		WebsocketClientEndpoint.Builder builder = getBuilder();
		assertNotNull(builder.toString());
	}
	
	@Test
	public void testBadUri() {
		try {
			WebsocketClientEndpoint.URI_BUILDER_FUNCTION.apply("//&@#^#ND");
			fail("Expected an IllegalArgumentException to be thrown if a bad URI is passed in");
		} catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof URISyntaxException);
		}
	}
	
	@Test
	public void testConnectionException() throws URISyntaxException {
		try {
			WebsocketClientEndpoint.URI_CONNECTION_CONSUMER.accept(endpoint, new UriWrapper("BadUri"));
			fail("Expected an IllegalStateException to be thrown if the connection fails");
		} catch(IllegalStateException e) {
			assertTrue(e.getCause() instanceof DeploymentException);
		}
	}

	protected abstract WebsocketClientEndpoint getEndpoint();
	protected abstract Builder getBuilder();
	
	@After
	public void tearDown() {
		Logger.getLogger(WebsocketClientEndpoint.class.getName()).setLevel(Level.INFO);
	}
}
