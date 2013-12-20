package com.kurento.kmf.jsonrpcconnector;

import com.kurento.kmf.jsonrpcconnector.internal.message.Request;

public interface JsonRpcHandler<P> {

	/**
	 * Invoked when a new JsonRpc request arrives.
	 * 
	 * @throws Exception
	 *             this method can handle or propagate exceptions.
	 */
	void handleRequest(Transaction transaction, Request<P> request)
			throws Exception;

	void afterConnectionEstablished(Session session) throws Exception;

	void afterConnectionClosed(Session session, String status) throws Exception;

	void handleTransportError(Session session, Throwable exception)
			throws Exception;

	void handleUncaughtException(Session session, Exception exception);
}
