package org.kurento.tree.server.kms;

public class WebRtc extends Element {

	protected WebRtc(Pipeline pipeline) {
		super(pipeline);
	}

	public String processSdpOffer(String sdpOffer) {
		return "fakeSdpResponse";
	}

}
