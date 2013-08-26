package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "CampusPartyPlayerPart", path = "/campusPartyPlayer", redirect = true, useControlProtocol = false)
public class CampusPartyPlayerPart implements PlayerHandler {

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		if (CampusPartyRtpPart.rtpEndPoint != null) {
			playRequest.play(CampusPartyRtpPart.rtpEndPoint);
		} else {
			playRequest.reject(500, "Inexistent RtpEndPoint to joint to");
		}
	}

	@Override
	public void onContentPlayed(PlayRequest playRequest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContentError(PlayRequest playRequest,
			ContentException exception) {
		// TODO Auto-generated method stub

	}

}
