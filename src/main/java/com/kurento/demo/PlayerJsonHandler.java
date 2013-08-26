package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;

@PlayerService(name = "JsonPlayerHandler", path = "/playerJson/*", redirect = false, useControlProtocol = true)
public class PlayerJsonHandler implements PlayerHandler {

	@Override
	public void onPlayRequest(final PlayRequest playRequest)
			throws ContentException {

		if (playRequest.getContentId() != null
				&& playRequest.getContentId().toLowerCase().startsWith("bar")) {
			playRequest.play("https://ci.kurento.com/video/barcodes.webm");
		} else {
			playRequest.play("http://media.w3.org/2010/05/sintel/trailer.webm");
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < 100; i++) {
					try {
						Thread.sleep(7000);
					} catch (InterruptedException e) {
					}
					if (i % 3 == 0) {
						((PlayRequestImpl) playRequest)
								.produceEvents(JsonRpcEvent.newEvent(
										"test-event-type", "test-event-data"));
					} else if (i % 3 == 1) {
						((PlayRequestImpl) playRequest).produceEvents(JsonRpcEvent
								.newEvent("url-event-type",
										"http://www.urjc.es"));
					} else {
						((PlayRequestImpl) playRequest)
								.produceEvents(JsonRpcEvent
										.newEvent("url-event-type",
												"http://www.gsyc.es/"));
					}
				}
			}
		}).start();

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
