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
import com.kurento.kmf.media.HttpEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.MixerPort;
import com.kurento.kmf.media.PlayerEndpoint;

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
	public MixerPort mixerPort1;
	public MixerPort mixerPort2;
	public MixerPort mixerPort3;
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

		mixerPort1 = mixer.newMixerPort().build();
		mixerPort2 = mixer.newMixerPort().build();
		mixerPort3 = mixer.newMixerPort().build();

		player2.connect(bn);
		player1.connect(mixerPort1);
		bn.connect(mixerPort2);

		// mixer.setMainEndPoint(mixerPort1);
		HttpEndpoint httpEndpoint = mediaPipeline.newHttpGetEndpoint()
				.terminateOnEOS().build();
		mixerPort3.connect(httpEndpoint);
		contentSession.start(httpEndpoint);
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		player1.play();
		player2.play();

		mixer.setSource(mixerPort1);
	}

	@Override
	public ContentCommandResult onContentCommand(
			HttpPlayerSession contentSession, ContentCommand contentCommand)
			throws Exception {
		if (contentCommand.getType().equalsIgnoreCase("player1")) {
			mixer.setSource(mixerPort1);
		} else if (contentCommand.getType().equalsIgnoreCase("player2")) {
			mixer.setSource(mixerPort2);
		}
		return new ContentCommandResult(contentCommand.getData());
	}
}