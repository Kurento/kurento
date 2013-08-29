package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.RtpMediaService;
import com.kurento.kmf.content.internal.rtp.RtpMediaRequestImpl;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.ZBarFilter;

@RtpMediaService(name = "CampusPartyRtpPartWithFilter", path = "/campusPartyRtpFilter")
public class CampusPartyRtpPartWithFilter implements RtpMediaHandler {

	private static final Logger log = LoggerFactory
			.getLogger(CampusPartyRtpPartWithFilter.class);

	public static ZBarFilter zBarFilterStaticReference = null;

	@Override
	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
		try {
			log.info("Recovering MediaPipelineFactory");
			MediaPipelineFactory mpf = request.getMediaPipelineFactory();
			log.info("Creating MediaPipeline");
			MediaPipeline mp = mpf.createMediaPipeline();
			((RtpMediaRequestImpl) request).addForCleanUp(mp);
			log.info("Creating ZBarFilter");
			ZBarFilter zbarFilter = mp.createFilter(ZBarFilter.class);
			request.startMedia(zbarFilter, null);
			zBarFilterStaticReference = zbarFilter;
		} catch (Throwable t) {
			zBarFilterStaticReference = null;
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
