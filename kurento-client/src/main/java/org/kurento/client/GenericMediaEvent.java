package org.kurento.client;

import org.kurento.jsonrpc.Props;

public class GenericMediaEvent extends MediaEvent {

	Props data;

	public GenericMediaEvent(@org.kurento.client.internal.server.Param("source") org.kurento.client.MediaObject source,
			@org.kurento.client.internal.server.Param("timestamp") String timestamp,
			@org.kurento.client.internal.server.Param("timestampMillis") String timestampMillis,
			@org.kurento.client.internal.server.Param("tags") java.util.List<org.kurento.client.Tag> tags,
			@org.kurento.client.internal.server.Param("type") String type,
			@org.kurento.client.internal.server.Param("genericData") Props data) {
		super(source, timestamp, timestampMillis, tags, type);
		this.data = data;
	}

	public Props getData() {
		return this.data;
	}

}
