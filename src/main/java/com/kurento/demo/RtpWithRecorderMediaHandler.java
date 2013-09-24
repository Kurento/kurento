package com.kurento.demo;

import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentService;
import com.kurento.kmf.content.RtpContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.RecorderEndPoint;

@RtpContentService(name = "RecordingRtpMediaHandler", path = "/rtpRecorder")
public class RtpWithRecorderMediaHandler extends RtpContentHandler {

	@Override
	public void onContentRequest(RtpContentSession session) throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);
		RecorderEndPoint recorderEndPoint = mp.createRecorderEndPoint(""); // TODO:
																			// set
																			// URI
		recorderEndPoint.record(); // TODO: is it necessary to invoke this?
		session.start(recorderEndPoint);
	}

}
