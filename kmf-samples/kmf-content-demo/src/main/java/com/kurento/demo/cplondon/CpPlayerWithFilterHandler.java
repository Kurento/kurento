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
package com.kurento.demo.cplondon;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

@HttpPlayerService(name = "CpPlayerWithJackVaderFilter", path = "/cpPlayerJack", redirect = true, useControlProtocol = true)
public class CpPlayerWithFilterHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);

		PlayerEndpoint playerEndPoint = mp.newPlayerEndpoint(
				"https://ci.kurento.com/video/fiwarecut.webm").build();

		JackVaderFilter filter = mp.newJackVaderFilter().build();
		playerEndPoint.connect(filter);
		session.setAttribute("player", playerEndPoint);
		HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		filter.connect(httpEndpoint);
		session.start(httpEndpoint);

	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		PlayerEndpoint playerendPoint = (PlayerEndpoint) session
				.getAttribute("player");
		playerendPoint.play();
	}
}
