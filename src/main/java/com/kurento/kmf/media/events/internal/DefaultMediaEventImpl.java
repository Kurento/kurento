package com.kurento.kmf.media.events.internal;

import com.kurento.kmf.media.events.DefaultMediaEvent;
import com.kurento.kms.thrift.api.KmsEvent;

/**
 * Default media event that is used when an unknown event type is received from
 * the media server
 * 
 * @author llopez
 * 
 */
public class DefaultMediaEventImpl extends AbstractMediaEvent implements
		DefaultMediaEvent {

	private byte[] data;

	public DefaultMediaEventImpl(KmsEvent event) {
		super(event);
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public void deserializeData(KmsEvent event) {
		data = event.getData();
	}

}
