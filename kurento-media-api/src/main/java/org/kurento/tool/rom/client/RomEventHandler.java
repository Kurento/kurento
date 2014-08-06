package org.kurento.tool.rom.client;

import org.kurento.jsonrpcconnector.Props;

public interface RomEventHandler {

	void processEvent(String objectRef, String subscription, String type,
			Props data);

}
