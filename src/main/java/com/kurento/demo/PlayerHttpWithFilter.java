package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kms.api.MediaType;

@PlayerService(name = "PlayerHttpWithFilter", path = "/playerHttpFilter")
public class PlayerHttpWithFilter implements PlayerHandler {

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		try {
			MediaPipelineFactory mpf = playRequest.getMediaPipelineFactory();
			MediaPipeline mp = mpf.createMediaPipeline();
			PlayerEndPoint playerEndPoint = mp.createUriEndPoint(
					PlayerEndPoint.class,
					"http://media.w3.org/2010/05/sintel/trailer.webm");
			ZBarFilter zBarFilter = mp.createFilter(ZBarFilter.class);
			playerEndPoint
					.getMediaSrcs(MediaType.VIDEO)
					.iterator()
					.next()
					.connect(
							zBarFilter.getMediaSinks(MediaType.VIDEO)
									.iterator().next());
			playRequest.usePlayer(playerEndPoint);
			playRequest.play(zBarFilter);

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
