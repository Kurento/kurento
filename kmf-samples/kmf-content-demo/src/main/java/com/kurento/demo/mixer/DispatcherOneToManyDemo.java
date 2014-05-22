/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package com.kurento.demo.mixer;

import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.DispatcherOneToMany;
import com.kurento.kmf.media.GStreamerFilter;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * HTTP Player Handler; tunnel strategy; no JSON control protocol.
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @version 1.0.1
 */
@HttpPlayerService(path = "/dispatcherOneToMany/*", redirect = true, useControlProtocol = true)
public class DispatcherOneToManyDemo extends HttpPlayerHandler {
	// MediaPipeline and MediaElements
	public MediaPipeline mediaPipeline;
	public PlayerEndpoint player1;
	public PlayerEndpoint player2;
	public DispatcherOneToMany mixer;
	public HubPort hubPort1;
	public HubPort hubPort2;
	public HubPort hubPort3;
	public GStreamerFilter bn;

	@Override
	public void onContentRequest(HttpPlayerSession contentSession)
			throws Exception {

		MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
		mediaPipeline = mpf.create();
		contentSession.releaseOnTerminate(mediaPipeline);
		player1 = mediaPipeline.newPlayerEndpoint(
				"http://ci.kurento.com/video/sintel.webm").build();
		player2 = mediaPipeline.newPlayerEndpoint(
				"http://ci.kurento.com/video/sintel.webm").build();
		bn = mediaPipeline.newGStreamerFilter("videobalance saturation=0.0")
				.build();

		mixer = mediaPipeline.newDispatcherOneToMany().build();

		hubPort1 = mixer.newHubPort().build();
		hubPort2 = mixer.newHubPort().build();
		hubPort3 = mixer.newHubPort().build();

		player2.connect(bn);
		player1.connect(hubPort1);
		bn.connect(hubPort2);

		// mixer.setMainEndPoint(hubPort1);
		HttpGetEndpoint httpEndpoint = mediaPipeline.newHttpGetEndpoint()
				.terminateOnEOS().build();
		hubPort3.connect(httpEndpoint);
		contentSession.start(httpEndpoint);
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		player1.play();
		player2.play();

		mixer.setSource(hubPort1);
	}

	@Override
	public ContentCommandResult onContentCommand(
			HttpPlayerSession contentSession, ContentCommand contentCommand)
			throws Exception {
		if (contentCommand.getType().equalsIgnoreCase("player1")) {
			mixer.setSource(hubPort1);
		} else if (contentCommand.getType().equalsIgnoreCase("player2")) {
			mixer.setSource(hubPort2);
		}
		return new ContentCommandResult(contentCommand.getData());
	}
}