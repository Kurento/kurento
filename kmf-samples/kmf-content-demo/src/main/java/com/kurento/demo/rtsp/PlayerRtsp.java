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
package com.kurento.demo.rtsp;

import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Rtsp player demo.
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @since 3.0.7
 */
@WebRtcContentService(path = "/playerRtsp/*")
public class PlayerRtsp extends WebRtcContentHandler {

	public PlayerEndpoint player;
	public MediaPipeline mediaPipeline;
	public WebRtcEndpoint webRtcEndpoint;

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
		mediaPipeline = mpf.create();
		contentSession.releaseOnTerminate(mediaPipeline);

		webRtcEndpoint = mediaPipeline.newWebRtcEndpoint().build();
		contentSession.start(webRtcEndpoint);
	}

	@Override
	public ContentCommandResult onContentCommand(
			WebRtcContentSession contentSession, ContentCommand contentCommand)
			throws Exception {
		if ("addRtsp".equalsIgnoreCase(contentCommand.getType())) {
			player = mediaPipeline.newPlayerEndpoint(contentCommand.getData())
					.build();

			player.connect(webRtcEndpoint);
			player.play();
		} else if ("pause".equalsIgnoreCase(contentCommand.getType())) {
			player.pause();
		} else if ("play".equalsIgnoreCase(contentCommand.getType())) {
			player.play();
		}
		return new ContentCommandResult(contentCommand.getData());
	}

}
