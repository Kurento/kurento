package com.kurento.kmf.media;

public class HttpEndPointEvent extends KmsEvent {

	public enum HttpEndPointEventType {
		GET_REQUEST, POST_REQUEST, UNEXPECTED_REQUEST
	}

	private final HttpEndPointEventType type;

	HttpEndPointEvent(HttpEndPoint source, HttpEndPointEventType type) {
		super(source);
		this.type = type;
	}

	public HttpEndPointEventType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "HttpEndPointEvent [type=" + type + "]";
	}

}
