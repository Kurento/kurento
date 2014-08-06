package org.kurento.client.internal.client;

import org.kurento.jsonrpc.Props;

public interface RomEventHandler {

	void processEvent(String objectRef, String subscription, String type,
			Props data);

}
