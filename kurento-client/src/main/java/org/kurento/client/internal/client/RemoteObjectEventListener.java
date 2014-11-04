package org.kurento.client.internal.client;

import org.kurento.jsonrpc.Props;

public interface RemoteObjectEventListener {
	public void onEvent(String eventType, Props data);
}