package com.kurento.kmf.content.internal.jsonrpc;

public class WebRtcJsonEvent {
	private String type;
	private String data;

	public static WebRtcJsonEvent newEvent(String type, String data) {
		return new WebRtcJsonEvent(type, data);
	}

	WebRtcJsonEvent() {
	}

	WebRtcJsonEvent(String type, String data) {
		this.type = type;
		this.data = data;
	}

	public String getType() {
		return type;
	}

	public String getData() {
		return data;
	}
}
