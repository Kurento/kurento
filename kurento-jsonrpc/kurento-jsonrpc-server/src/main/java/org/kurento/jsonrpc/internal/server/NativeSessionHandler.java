package org.kurento.jsonrpc.internal.server;

import org.kurento.jsonrpc.Session;

/**
 * This interface will be implemented by JsonRpcHandlers that want a low level
 * handling of requests with unknown sessionId. It is specially useful in
 * clustered environments when session can be stored in other data structures
 * 
 * @author micael.gallego@gmail.com
 */
public interface NativeSessionHandler {

	public boolean isSessionKnown(String sessionId);
	
	public void processNewCreatedSession(Session session);

}
