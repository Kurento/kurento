package com.kurento.kmf.media.events;

import org.apache.thrift.protocol.TProtocol;

import com.kurento.kms.thrift.api.MediaEvent;

public class HttpEvent extends ThriftSerializedMediaEvent {

	public HttpEvent(MediaEvent event) {
		super(event);
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		// TODO Auto-generated method stub

	}

}
