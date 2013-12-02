package com.kurento.demo.campusparty;

import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentService;
import com.kurento.kmf.content.RtpContentSession;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;

@RtpContentService(name = "CpRtpWithFilter", path = "/cpRtpJack")
public class CpRtcRtpJackHandler extends RtpContentHandler {

	public static JackVaderFilter sharedFilterReference = null;

	@Override
	public void onContentRequest(RtpContentSession session) throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);
		JackVaderFilter filter = mp.newJackVaderFilter().build();
		session.start(filter);
		sharedFilterReference = filter;
	}

}
