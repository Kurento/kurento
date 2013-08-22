package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "MyPlayerHandlerRejectWithRedirect", path = "/player-reject-with-redirect", redirect = true)
public class MyPlayerHandlerRejectWithRedirect implements PlayerHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyPlayerHandlerRejectWithRedirect.class);

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		log.debug("onPlayRequest");
		playRequest.reject(407, "Reject in player handler (with redirect)");
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
