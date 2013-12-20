package com.kurento.kmf.jsonrpcconnector;

public abstract class DefaultJsonRpcHandler<P> implements JsonRpcHandler<P> {

	@Override
	public void afterConnectionEstablished(Session session) throws Exception {
	}

	@Override
	public void afterConnectionClosed(Session session, String status)
			throws Exception {
	}

	@Override
	public void handleTransportError(Session session, Throwable exception)
			throws Exception {
	}

	@Override
	public void handleUncaughtException(Session session, Exception exception) {
	}
}
