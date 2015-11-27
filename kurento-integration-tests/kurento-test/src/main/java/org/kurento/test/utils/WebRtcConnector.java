package org.kurento.test.utils;

import org.kurento.client.WebRtcEndpoint;

public interface WebRtcConnector {

	public void connect(WebRtcEndpoint inputEndpoint,
			WebRtcEndpoint outputEndpoint);

}
