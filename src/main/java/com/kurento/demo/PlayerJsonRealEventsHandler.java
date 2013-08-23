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

@PlayerService(name = "PlayerJsonRealEventsHandler", path = "/playerWithEvents", useControlProtocol = true)
public class PlayerJsonRealEventsHandler implements PlayerHandler {

	private String fuckingUrl = "";

	@Override
	public void onPlayRequest(final PlayRequest playRequest)
			throws ContentException {
		try {
			MediaPipelineFactory mpf = playRequest.getMediaPipelineFactory();
			MediaPipeline mp = mpf.createMediaPipeline();
			((PlayRequestImpl) playRequest).addForCleanUp(mp);

			PlayerEndPoint playerEndPoint = mp.createUriEndPoint(
					PlayerEndPoint.class,
					"https://ci.kurento.com/video/barcodes.webm");
			ZBarFilter filter = mp.createFilter(ZBarFilter.class);
			playerEndPoint
					.getMediaSrcs(MediaType.VIDEO)
					.iterator()
					.next()
					.connect(
							filter.getMediaSinks(MediaType.VIDEO).iterator()
									.next());

			filter.addListener(new MediaEventListener<ZBarEvent>() {

				@Override
				public void onEvent(ZBarEvent event) {
					System.out.println("******************************* Event "
							+ event.getType() + "-->" + event.getValue());
					if (fuckingUrl.equals(event.getValue())) {
						return;
					} else {
						fuckingUrl = event.getValue();
						((PlayRequestImpl) playRequest)
								.produceEvents(JsonRpcEvent.newEvent(
										event.getType(), event.getValue()));
					}
				}
			});

			playRequest.play(filter);

			playerEndPoint.play();

		} catch (Throwable t) {
			playRequest.reject(0, "Error " + t.getMessage());
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
