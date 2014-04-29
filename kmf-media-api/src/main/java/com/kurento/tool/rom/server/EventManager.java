package com.kurento.tool.rom.server;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.media.events.Event;

public class EventManager {

	private Session session;

	public EventManager(Session session) {
		this.session = session;
	}

	public void fireEvent(Event sampleEventImpl) throws IOException {

		JsonObject eventAsJsonObject = JsonUtils.toJsonObject(sampleEventImpl);
		JsonObject params = new JsonObject();
		params.addProperty("eventType", sampleEventImpl.getClass()
				.getSimpleName());
		params.add("eventData", eventAsJsonObject);

		session.sendNotification("onEvent", params);
	}

}
