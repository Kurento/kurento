package com.kurento.kmf.media.events;

import com.kurento.kms.thrift.api.MediaEvent;

public class HttpEvent extends KmsEvent {

	public HttpEvent(MediaEvent event) {
		super(event);
	}
	//
	// public enum HttpEndPointEventType {
	// GET_REQUEST, POST_REQUEST, UNEXPECTED_REQUEST
	// }
	//
	// private final HttpEndPointEventType type;
	//
	// zHttpEndPointEvent(zHttpEndPoint source, HttpEndPointEventType type) {
	// super(source);
	// this.type = type;
	// }
	//
	// public HttpEndPointEventType getType() {
	// return type;
	// }
	//
	// @Override
	// public String toString() {
	// return "HttpEndPointEvent [type=" + type + "]";
	// }
	//
}
