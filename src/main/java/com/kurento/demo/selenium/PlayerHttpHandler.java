package com.kurento.demo.selenium;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "SimplePlayerHandler", path = "/playerJson", redirect = true, useControlProtocol = true)
public class PlayerHttpHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		playRequest.play("https://ci.kurento.com/video/sintel.webm");
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
