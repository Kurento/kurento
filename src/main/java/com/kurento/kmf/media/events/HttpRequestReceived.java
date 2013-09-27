package com.kurento.kmf.media.events;

import com.kurento.kms.thrift.api.HttpRequestReceivedData;

public interface HttpRequestReceived extends MediaEvent {
	HttpRequestReceivedData getData();
}
