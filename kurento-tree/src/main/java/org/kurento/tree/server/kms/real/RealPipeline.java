package org.kurento.tree.server.kms.real;

import org.kurento.client.MediaPipeline;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;

public class RealPipeline extends Pipeline {

	private MediaPipeline mediaPipeline;

	public RealPipeline(RealKms realKms) {
		super(realKms);
		mediaPipeline = MediaPipeline.with(realKms.getKurentoClient()).create();
	}

	public MediaPipeline getMediaPipeline() {
		return mediaPipeline;
	}

	protected WebRtc newWebRtc() {
		return new RealWebRtc(this);
	}

	protected Plumber newPlumber() {
		return new RealPlumber(this);
	}

}
