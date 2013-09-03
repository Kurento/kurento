package com.kurento.demo.campusparty;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "CpRtcPlayerHandler", path = "/cpRtcPlayerJack", useControlProtocol = true)
public class CpRtcPlayerJackHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		if (CpRtcRtpJackHandler.sharedFilterReference == null) {

			playRequest.reject(500, "Rtp session has not been established");

		} else {

			playRequest.play(CpRtcRtpJackHandler.sharedFilterReference);

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
