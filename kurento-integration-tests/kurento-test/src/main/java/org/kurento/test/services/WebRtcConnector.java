package org.kurento.test.services;

import org.kurento.client.WebRtcEndpoint;

public interface WebRtcConnector {

	public void connect(WebRtcEndpoint inputEndpoint,
			WebRtcEndpoint outputEndpoint);

}
