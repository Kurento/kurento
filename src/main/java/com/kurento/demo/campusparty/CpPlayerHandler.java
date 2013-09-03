package com.kurento.demo.campusparty;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;

@PlayerService(name = "PlayerJson", path = "/cpPlayer", useControlProtocol = true)
public class CpPlayerHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		try {
			MediaPipelineFactory mpf = playRequest.getMediaPipelineFactory();
			
			MediaPipeline mp = mpf.createMediaPipeline();
			((PlayRequestImpl) playRequest).addForCleanUp(mp);
			
			PlayerEndPoint playerEndPoint = mp.createUriEndPoint(
					PlayerEndPoint.class, "https://ci.kurento.com/video/fiwarecut.webm");
			
			playRequest.usePlayer(playerEndPoint);
			
			playRequest.play(playerEndPoint);
			
		} catch (Throwable t) {
			playRequest.reject(500, t.getMessage());
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
