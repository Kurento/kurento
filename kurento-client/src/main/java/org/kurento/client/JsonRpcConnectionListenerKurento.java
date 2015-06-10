package org.kurento.client;

import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;

public class JsonRpcConnectionListenerKurento implements
		JsonRpcWSConnectionListener {

	private KurentoConnectionListener listener;

	public JsonRpcConnectionListenerKurento(KurentoConnectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void connectionFailed() {
		listener.connectionFailed();
	}

	@Override
	public void connected() {
		listener.connected();
	}

	@Override
	public void disconnected() {
		listener.disconnected();
	}

	public static JsonRpcWSConnectionListener create(
			KurentoConnectionListener listener) {

		if (listener == null) {
			return null;
		}

		return new JsonRpcConnectionListenerKurento(listener);
	}

}
