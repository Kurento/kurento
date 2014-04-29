/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.demo.campusparty;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * HTTP Player Handler; tunnel strategy; JSON control protocol; it creates a
 * player end point in the media server with a WEBM video, and a Jack Vader
 * Filter is connected to this player. This filter detects human faces an put
 * them a pirate hut.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
@HttpPlayerService(name = "PlayerJsonEvents", path = "/playerJsonEvents", redirect = true, useControlProtocol = true)
public class PlayerJsonWithFilterHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {

		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();

		PlayerEndpoint player = mp.newPlayerEndpoint(
				"https://ci.kurento.com/video/barcodes.webm").build();
		session.setAttribute("player", player);

		ZBarFilter zBarFilter = mp.newZBarFilter().build();
		player.connect(zBarFilter);
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		zBarFilter.connect(httpEP);
		session.start(httpEP);
		session.setAttribute("eventValue", "");
		zBarFilter
				.addCodeFoundListener(new MediaEventListener<CodeFoundEvent>() {

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
		PlayerEndpoint playerEndpoint = (PlayerEndpoint) session
				.getAttribute("player");
		playerEndpoint.play();
	}

}
