package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.RtpMediaService;
import com.kurento.kmf.content.internal.rtp.RtpMediaRequestImpl;
import com.kurento.kmf.media.RtpEndPoint;

@RtpMediaService(name="CampusPartyRtpPart", path="/campusPartyRtp")
public class CampusPartyRtpPart implements RtpMediaHandler{

	public static RtpEndPoint rtpEndPoint;
	
	@Override
	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
		request.startMedia(null, null);
		rtpEndPoint = ((RtpMediaRequestImpl)request).getRtpEndPoint();
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
