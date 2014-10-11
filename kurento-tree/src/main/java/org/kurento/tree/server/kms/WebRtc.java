package org.kurento.tree.server.kms;

public class WebRtc extends Element {

	WebRtc(Pipeline pipeline) {
		super(pipeline);
	}

	public String processSdpOffer(String sdpOffer) {
		return "fakeSdpResponse";
	}

}
