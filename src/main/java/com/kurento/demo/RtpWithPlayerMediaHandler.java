package com.kurento.demo;

import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentService;
import com.kurento.kmf.content.RtpContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndPoint;

@RtpContentService(name = "PlayingRtpMediaHandler", path = "/rtpPlayer")
public class RtpWithPlayerMediaHandler extends RtpContentHandler {

	@Override
	public void onContentRequest(RtpContentSession session) throws Exception {
		MediaPipeline mediaPipeline = null;
		mediaPipeline = session.getMediaPipelineFactory().create();
		session.releaseOnTerminate(mediaPipeline);

		PlayerEndPoint player = mediaPipeline.newPlayerEndPoint(
				"https://ci.kurento.com/video/barcodes.webm").build();
		session.setAttribute("player", player);
		session.start(null);
	}

	@Override
	public void onContentStarted(RtpContentSession session) {
		PlayerEndPoint playerendPoint = (PlayerEndPoint) session
				.getAttribute("player");
		playerendPoint.play();
	}

}
