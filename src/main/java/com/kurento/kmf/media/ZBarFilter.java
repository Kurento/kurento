package com.kurento.kmf.media;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kms.api.FilterType;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaObjectId;

public class ZBarFilter extends Filter {
	public static final Logger log = LoggerFactory.getLogger(ZBarFilter.class);

	private static final long serialVersionUID = 1L;

	static final FilterType filterType = FilterType.ZBAR_FILTER;

	ZBarFilter(MediaObjectId filterId) {
		super(filterId);
	}

	public MediaEventListener<ZBarEvent> addListener(
			MediaEventListener<ZBarEvent> listener) {
		return handler.addListener(this, listener);
	}

	public boolean removeListener(MediaEventListener<ZBarEvent> listener) {
		return handler.removeListener(this, listener);
	}

	@Override
	KmsEvent deserializeEvent(MediaEvent event) {
		try {
			TProtocol prot = handler.getProtocolFromEvent(event);

			com.kurento.kms.api.ZBarEvent thriftEvent = new com.kurento.kms.api.ZBarEvent();
			thriftEvent.read(prot);

			return new ZBarEvent(this, thriftEvent.getType(),
					thriftEvent.getValue());

		} catch (TException e) {
			log.error(
					"Error deserializing player event, falling back to default deserializer"
							+ e, e);
		}

		return super.deserializeEvent(event);
	}
}
