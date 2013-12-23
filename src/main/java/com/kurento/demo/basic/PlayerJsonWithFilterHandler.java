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
package com.kurento.demo.basic;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndpoint;
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

		PlayerEndpoint player = mp.newPlayerEndpoint(
				"https://ci.kurento.com/video/barcodes.webm").build();
		session.setAttribute("player", player);

		ZBarFilter zBarFilter = mp.newZBarFilter().build();
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
		PlayerEndpoint PlayerEndpoint = (PlayerEndpoint) session
				.getAttribute("player");
		PlayerEndpoint.play();
	}
}
