package com.kurento.kmf.media.events.internal;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.HttpRequestReceived;
import com.kurento.kmf.media.internal.ProvidesMediaElement;
import com.kurento.kms.thrift.api.HttpRequestReceivedData;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.mediaEventDataTypesConstants;

@ProvidesMediaElement(type = mediaEventDataTypesConstants.HTTP_REQUEST_RECEIVED)
public class HttpRequestReceivedImpl extends ThriftSerializedMediaEvent
		implements HttpRequestReceived {

	private HttpRequestReceivedData data;

	public HttpRequestReceivedImpl(KmsEvent event) {
		super(event);
	}

	@Override
	public HttpRequestReceivedData getData() {
		return data;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			data.read(pr);
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO
		}
	}

}
