package com.kurento.kmf.media.events;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.IsMediaEvent;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kms.thrift.api.Event;

@IsMediaEvent(type = ZBarEvent.TYPE)
public class ZBarEvent extends ThriftSerializedMediaEvent {

	public static final String TYPE = "ZBarEvent";

	private String data;

	public ZBarEvent(Event event) {
		super(event);
	}

	@Override
	public ZBarFilter getSource() {
		return (ZBarFilter) super.getSource();
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

	public String getData() {
		return data;
	}
}
