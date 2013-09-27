package com.kurento.kmf.media.events.internal;

import org.apache.thrift.protocol.TProtocol;

import com.kurento.kms.thrift.api.KmsEvent;

public class VoidMediaEvent extends ThriftSerializedMediaEvent {

	public VoidMediaEvent(KmsEvent event) {
		super(event);
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
	}

}
