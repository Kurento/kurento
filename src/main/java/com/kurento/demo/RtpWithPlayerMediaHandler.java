package com.kurento.demo;

import java.io.IOException;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.RtpMediaService;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndPoint;

@RtpMediaService(name = "PlayingRtpMediaHandler", path = "/rtpPlayer")
public class RtpWithPlayerMediaHandler implements RtpMediaHandler {

	@Override
	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
		MediaPipeline mediaPipeline = null;
		try {
			mediaPipeline = request.getMediaPipelineFactory()
					.createMediaPipeline();

			PlayerEndPoint player = mediaPipeline.createUriEndPoint(
					PlayerEndPoint.class,
					"https://ci.kurento.com/video/barcodes.webm");

			request.startMedia(null, player);

			player.play();

		} catch (Exception e) {
			try {
				mediaPipeline.release();
			} catch (IOException e1) {
			}
			throw new ContentException(e.getMessage());
		}

	}

	@Override
	public void onMediaTerminated(String requestId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMediaError(String requestId, ContentException exception) {
		// TODO Auto-generated method stub

	}

}
