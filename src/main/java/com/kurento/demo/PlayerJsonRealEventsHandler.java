package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@HttpPlayerService(name = "PlayerJsonRealEventsHandler", path = "/playerWithEvents", redirect = true, useControlProtocol = true)
public class PlayerJsonRealEventsHandler extends HttpPlayerHandler {

	private String url = "";
	private static final Logger log = LoggerFactory
			.getLogger(PlayerJsonRealEventsHandler.class);

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);

		PlayerEndPoint playerEndPoint = mp
				.createPlayerEndPoint("https://ci.kurento.com/video/barcodes.webm");
		ZBarFilter filter = mp.createZBarFilter();
		playerEndPoint.connect(filter);

		filter.addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {

			@Override
			public void onEvent(CodeFoundEvent event) {
				log.info("******************************* Event "
						+ event.getType() + "-->" + event.getValue());
				if (url.equals(event.getValue())) {
					return;
				} else {
					url = event.getValue();
					session.publishEvent(new ContentEvent(event.getType(),
							event.getValue()));
				}
			}
		});
		session.setAttribute("player", playerEndPoint);
		session.start(filter);
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		PlayerEndPoint playerendPoint = (PlayerEndPoint) session
				.getAttribute("player");
		playerendPoint.play();
	}
}
