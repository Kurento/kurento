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

import java.util.ArrayList;
import java.util.List;

import com.kurento.demo.internal.VideoURLs;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.Composite;
import com.kurento.kmf.media.GStreamerFilter;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.MediaProfileSpecType;
import com.kurento.kmf.media.PlayerEndpoint;

/**
 * HTTP Player Handler; tunnel strategy; no JSON control protocol.
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.1
 */
@HttpPlayerService(path = "/compositeMixer/*", redirect = true, useControlProtocol = true)
public class CompositeMixerDemo extends HttpPlayerHandler {

	static class PlayerConnection {
		public PlayerEndpoint player;
		public HubPort port;

		public PlayerConnection(PlayerEndpoint p, HubPort mp) {
			this.player = p;
			this.port = mp;
		}
	}

	// MediaPipeline and MediaElements
	public MediaPipeline mediaPipeline;
	public Composite mixer;
	List<PlayerConnection> playersList;

	@Override
	public void onContentRequest(HttpPlayerSession contentSession)
			throws Exception {
		HubPort mixerPort1;
		HubPort mixerPort2;
		HubPort mixerPort3;
		PlayerEndpoint player1;
		PlayerEndpoint player2;
		GStreamerFilter bn;

		MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
		mediaPipeline = mpf.create();
		contentSession.releaseOnTerminate(mediaPipeline);
		player1 = mediaPipeline.newPlayerEndpoint(
				VideoURLs.map.get("small-mp4")).build();
		player2 = mediaPipeline.newPlayerEndpoint(
				VideoURLs.map.get("small-mp4")).build();
		bn = mediaPipeline.newGStreamerFilter("videobalance saturation=0.0")
				.build();

		mixer = mediaPipeline.newComposite().build();

		mixerPort1 = mixer.newHubPort().build();
		mixerPort2 = mixer.newHubPort().build();
		mixerPort3 = mixer.newHubPort().build();

		player2.connect(bn);
		player1.connect(mixerPort1);
		bn.connect(mixerPort2);

		playersList = new ArrayList<PlayerConnection>();

		PlayerConnection conection1 = new PlayerConnection(player1, mixerPort1);
		PlayerConnection conection2 = new PlayerConnection(player2, mixerPort2);

		playersList.add(conection1);
		playersList.add(conection2);

		// mixer.setMainEndPoint(mixerPort1);
		HttpGetEndpoint httpEndpoint = mediaPipeline.newHttpGetEndpoint()
		// .withMediaProfile(MediaProfileSpecType.MP4).build();
				.withMediaProfile(MediaProfileSpecType.WEBM).build();
		mixerPort3.connect(httpEndpoint);
		contentSession.start(httpEndpoint);
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {

		for (PlayerConnection p : playersList) {
			p.player.play();
		}
	}

	@Override
	public ContentCommandResult onContentCommand(
			HttpPlayerSession contentSession, ContentCommand contentCommand)
			throws Exception {
		if (contentCommand.getType().equalsIgnoreCase("newplayer")) {
			PlayerEndpoint player = mediaPipeline.newPlayerEndpoint(
					VideoURLs.map.get("small-mp4")).build();
			HubPort mixerPort = mixer.newHubPort().build();
			player.connect(mixerPort);
			player.play();
			PlayerConnection conection = new PlayerConnection(player, mixerPort);
			playersList.add(conection);
		} else if (contentCommand.getType().equalsIgnoreCase("deleteplayer")) {
			PlayerConnection connection = playersList.get(0);
			connection.player.stop();
			connection.player.release();
			connection.port.release();
			playersList.remove(0);
		}
		return new ContentCommandResult(contentCommand.getData());
	}

	@Override
	public void onSessionTerminated(HttpPlayerSession contentSession, int code,
			String reason) throws Exception {
		super.onSessionTerminated(contentSession, code, reason);
	}
}
