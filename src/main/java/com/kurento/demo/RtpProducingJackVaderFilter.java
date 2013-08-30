package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.RtpMediaService;
import com.kurento.kmf.content.internal.rtp.RtpMediaRequestImpl;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;

@RtpMediaService(name = "RtpProducingJackVaderFilter", path = "/rtpJack")
public class RtpProducingJackVaderFilter implements RtpMediaHandler {
	private static final Logger log = LoggerFactory
			.getLogger(RtpProducingJackVaderFilter.class);

	public static JackVaderFilter sharedJackVaderReference = null;

	@Override
	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
		try {
			MediaPipelineFactory mpf = request.getMediaPipelineFactory();
			MediaPipeline mp = mpf.createMediaPipeline();
			((RtpMediaRequestImpl) request).addForCleanUp(mp);
			JackVaderFilter filter = mp.createFilter(JackVaderFilter.class);
			request.startMedia(filter, null);
			sharedJackVaderReference = filter;
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			request.reject(500, t.getMessage());
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
