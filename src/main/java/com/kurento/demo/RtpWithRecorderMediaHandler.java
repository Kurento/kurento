package com.kurento.demo;

import java.io.IOException;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.RtpMediaService;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.RecorderEndPoint;

@RtpMediaService(name = "RecordingRtpMediaHandler", path = "/rtpRecorder")
public class RtpWithRecorderMediaHandler implements RtpMediaHandler {

	@Override
	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
		MediaPipelineFactory mpf = request.getMediaPipelineFactory();
		MediaPipeline mp = null;
		try {
			mp = mpf.createMediaPipeline();
			RecorderEndPoint recorderEndPoint = mp.createUriEndPoint(
					RecorderEndPoint.class, ""); // TODO: set URI
			recorderEndPoint.record(); // TODO: is it necessary to invoke this?
			request.startMedia(recorderEndPoint, null);
		} catch (Throwable t) {
			try {
				mp.release();
			} catch (IOException e) {
			}
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
