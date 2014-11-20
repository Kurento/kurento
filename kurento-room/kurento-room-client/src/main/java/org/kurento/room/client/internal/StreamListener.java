package org.kurento.room.client.internal;

import java.util.Map;

public interface StreamListener {

	void attributeUpdateEvent(Map<String, Object> atts);

	void dataEvent(String message);

}
