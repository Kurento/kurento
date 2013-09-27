package com.kurento.kmf.media.events.internal;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kmf.media.internal.ZBarFilterImpl;
import com.kurento.kms.thrift.api.KmsEvent;

@ProvidesMediaEvent(type = ZBarEvent.TYPE)
public class ZBarEvent extends ThriftSerializedMediaEvent {

	// TODO Fix TYPE to something like StringEvent or other preconfigured event
	public static final String TYPE = "ZBarEvent";

	private String data;

	public ZBarEvent(KmsEvent event) {
		super(event);
	}

	@Override
	public ZBarFilterImpl getSource() {
		return (ZBarFilterImpl) super.getSource();
	}

	public String getData() {
		return data;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			data = pr.readString();
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

}
