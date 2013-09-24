package com.kurento.kmf.media.events;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.IsMediaEvent;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kms.thrift.api.MediaEvent;

@IsMediaEvent(type = "ZbarEvent")
public class ZBarEvent extends ThriftSerializedMediaEvent {

	private String data;

	public ZBarEvent(MediaEvent event) {
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
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO:
																				// code
		}
	}

	public String getData() {
		return data;
	}
}
