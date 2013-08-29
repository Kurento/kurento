package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;
import com.kurento.kmf.media.MediaEventListener;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.ZBarEvent;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kms.api.MediaType;

@PlayerService(name = "PlayerJsonWithFilterHandler", path = "/playerJsonFilter/*", useControlProtocol = true)
public class PlayerJsonWithFilterHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(final PlayRequest playRequest)
			throws ContentException {
		try {

			MediaPipelineFactory mpf = playRequest.getMediaPipelineFactory();
			MediaPipeline mp = mpf.createMediaPipeline();

			PlayerEndPoint player = mp.createUriEndPoint(PlayerEndPoint.class,
					"https://ci.kurento.com/video/barcodes.webm");
			ZBarFilter zBarFilter = mp.createFilter(ZBarFilter.class);
			player.getMediaSrcs(MediaType.VIDEO)
					.iterator()
					.next()
					.connect(
							zBarFilter.getMediaSinks(MediaType.VIDEO)
									.iterator().next());
			playRequest.usePlayer(player);
			playRequest.play(zBarFilter);
			playRequest.setAttribute("eventValue", "");
			zBarFilter.addListener(new MediaEventListener<ZBarEvent>() {
				@Override
				public void onEvent(ZBarEvent event) {
					if (playRequest.getAttribute("eventValue").toString()
							.equals(event.getValue())) {
						return;
					}
					playRequest.setAttribute("eventValue", event.getValue());
					((PlayRequestImpl) playRequest).produceEvents(JsonRpcEvent
							.newEvent(event.getType(), event.getValue()));
				}
			});

		} catch (Throwable t) {

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
