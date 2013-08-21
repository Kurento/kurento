package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "SimplePlayerHandler", path = "/playerHttp/*", redirect = true, useControlProtocol = false)
public class PlayerHttpHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		if (playRequest.getContentId() != null
				&& playRequest.getContentId().toLowerCase().startsWith("bar")) {
			playRequest.play("https://ci.kurento.com/video/barcodes.webm");
		} else {
			playRequest.play("http://media.w3.org/2010/05/sintel/trailer.webm");
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
