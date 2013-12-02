package com.kurento.demo.campusparty;

import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentService;
import com.kurento.kmf.content.RtpContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.ZBarFilter;

@RtpContentService(name = "CpRtcRtpZbarHandler", path = "/cpRtpZbar")
public class CpRtcRtpZbarHandler extends RtpContentHandler {

	public static ZBarFilter sharedFilterReference = null;

	@Override
	public void onContentRequest(RtpContentSession session) throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);
		ZBarFilter filter = mp.newZBarFilter().build();
		session.start(filter);
		sharedFilterReference = filter;
	}

}
