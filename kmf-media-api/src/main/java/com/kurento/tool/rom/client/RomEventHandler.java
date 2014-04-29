package com.kurento.tool.rom.client;

import com.kurento.kmf.jsonrpcconnector.Props;

public interface RomEventHandler {

	void processEvent(String objectRef, String subscription, String type,
			Props data);

}
