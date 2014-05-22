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
package com.kurento.demo.player;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * HTTP Player Handler; tunnel strategy; JSON control protocol.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpPlayerService(path = "/playerJsonSwitch/*")
public class PlayerJsonSwitch extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession contentSession)
			throws Exception {
		// contentSession.start("https://ci.kurento.com/video/sintel.webm");

		MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
		final MediaPipeline mp = mpf.create();
		contentSession.releaseOnTerminate(mp);
		PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint(
				"http://ci.kurento.com/video/sintel.webm").build();
		HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEndpoint.connect(httpEndpoint);
		contentSession.setAttribute("player", playerEndpoint);
		contentSession.start(httpEndpoint);

		playerEndpoint
				.addEndOfStreamListener(new MediaEventListener<EndOfStreamEvent>() {
					@Override
					public void onEvent(EndOfStreamEvent event) {
						PlayerEndpoint newPlayerEndpoint = mp
								.newPlayerEndpoint(
										"http://media.w3.org/2010/05/sintel/trailer.webm")
								.build();
						newPlayerEndpoint.play();
						// contentSession
						// .setAttribute("player", newPlayerEndpoint);
						// contentSession.start(newPlayerEndpoint);
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
