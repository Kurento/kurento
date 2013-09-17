package com.kurento.kmf.media.events;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import java.nio.ByteBuffer;

import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kms.thrift.api.MediaEvent;

public class KmsEvent {

	private final MediaObjectRefDTO objectRef;

	private final String type;

	private final ByteBuffer data;

	public KmsEvent(MediaEvent event) {
		this.objectRef = fromThrift(event.source);
		this.type = event.type;
		// TODO is this ok?
		this.data = event.data;
	}

	public MediaObjectRefDTO getObjectRef() {
		return this.objectRef;
	}

	public String getType() {
		return this.type;
	}

	public ByteBuffer getData() {
		return data;
	}

}
