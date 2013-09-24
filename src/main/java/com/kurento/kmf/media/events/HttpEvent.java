package com.kurento.kmf.media.events;

import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.media.IsMediaEvent;
import com.kurento.kms.thrift.api.KmsEvent;

@IsMediaEvent(type = HttpEvent.TYPE)
public class HttpEvent extends ThriftSerializedMediaEvent {

	public static final String TYPE = "HttpEvent";

	public HttpEvent(KmsEvent event) {
		super(event);
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		// TODO add implementation
	}

}
