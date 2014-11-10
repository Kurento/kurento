package org.kurento.tree.server.kms.real;

import org.kurento.client.WebRtcEndpoint;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.WebRtc;

public class RealWebRtc extends WebRtc implements RealElement {

	private WebRtcEndpoint webRtcEndpoint;

	public RealWebRtc(RealPipeline pipeline) {
		super(pipeline);
		this.webRtcEndpoint = new WebRtcEndpoint.Builder(
				pipeline.getMediaPipeline()).build();
	}

	@Override
	public String processSdpOffer(String sdpOffer) {
		return webRtcEndpoint.processOffer(sdpOffer);
	}

	@Override
	public void release() {
		super.release();
		webRtcEndpoint.release();
	}

	@Override
	public WebRtcEndpoint getMediaElement() {
		return webRtcEndpoint;
	}

	@Override
	public void connect(Element element) {
		if (!(element instanceof RealElement)) {
			throw new RuntimeException(
					"A real element can not be connected to non real one");
		}
		super.connect(element);
		webRtcEndpoint.connect(((RealElement) element).getMediaElement());
	}
}
