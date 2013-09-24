package com.kurento.demo;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

@HttpPlayerService(name = "PlayerJsonWithFilterHandler", path = "/playerJsonFilter/*", redirect = true, useControlProtocol = true)
public class PlayerJsonWithFilterHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {

		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();

		PlayerEndPoint player = mp
				.createPlayerEndPoint("https://ci.kurento.com/video/barcodes.webm");
		session.setAttribute("player", player);

		ZBarFilter zBarFilter = mp.createZBarFilter();
		player.connect(zBarFilter);
		session.start(zBarFilter);
		session.setAttribute("eventValue", "");
		zBarFilter
				.addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {

					@Override
					public void onEvent(CodeFoundEvent event) {
						if (session.getAttribute("eventValue").toString()
								.equals(event.getValue())) {
							return;
						}
						session.setAttribute("eventValue", event.getValue());
						session.publishEvent(new ContentEvent(event.getType(),
								event.getValue()));
					}
				});

	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		PlayerEndPoint playerendPoint = (PlayerEndPoint) session
				.getAttribute("player");
		playerendPoint.play();
	}
}
