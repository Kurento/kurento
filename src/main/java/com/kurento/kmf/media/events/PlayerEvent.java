package com.kurento.kmf.media.events;

import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.media.IsMediaEvent;
import com.kurento.kms.thrift.api.KmsEvent;

@IsMediaEvent(type = PlayerEvent.TYPE)
public class PlayerEvent extends ThriftSerializedMediaEvent {

	// TODO Fix TYPE to something like StringEvent or other preconfigured event
	public static final String TYPE = "PlayerEvent";

	PlayerEvent(KmsEvent event) {
		super(event);
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		// TODO add implementation
	}

}
