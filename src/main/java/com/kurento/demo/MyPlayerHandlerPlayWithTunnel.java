package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "MyPlayerHandlerPlayWithTunnel", path = "/player-play-with-tunnel", redirect = false)
public class MyPlayerHandlerPlayWithTunnel implements PlayerHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyPlayerHandlerPlayWithTunnel.class);

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		log.debug("onPlayRequest");
		playRequest.play("small.webm");
	}

	@Override
	public void onContentPlayed(PlayRequest playRequest) {
		log.debug("onContentPlayed");
	}

	@Override
	public void onContentError(PlayRequest playRequest,
			ContentException exception) {
		log.debug("onContentError " + exception.getMessage());
	}

}
