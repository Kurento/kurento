package org.kurento.room.client.internal;

import java.util.Map;

public class LocalStreamInfo {

	private Map<String, Object> attributes;
	private String sdpOffer;

	public LocalStreamInfo(Map<String, Object> attributes, String sdpOffer) {
		this.attributes = attributes;
		this.sdpOffer = sdpOffer;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public String getSdpOffer() {
		return sdpOffer;
	}
}
