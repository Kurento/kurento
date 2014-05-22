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
package com.kurento.demo.playerjson;

import com.kurento.demo.internal.EventListener;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * HTTP Player Handler; tunnel strategy; JSON control protocol; plays a
 * non-existing Media Element.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
@HttpPlayerService(path = "/playerFlowBadMediaElement", redirect = false, useControlProtocol = true)
public class PlayerJsonBadMediaElement extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession contentSession)
			throws Exception {
		EventListener.clearEventList();
		EventListener.addEvent();

		MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		contentSession.releaseOnTerminate(mp);
		PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint("").build();
		contentSession.setAttribute("player", playerEndpoint);
		HttpGetEndpoint httpEP = mp.newHttpGetEndpoint().terminateOnEOS()
				.build();
		playerEndpoint.connect(httpEP);
		contentSession.start(httpEP);
	}

	@Override
	public void onContentStarted(HttpPlayerSession contentSession)
			throws Exception {
		EventListener.addEvent();
		PlayerEndpoint playerEndpoint = (PlayerEndpoint) contentSession
				.getAttribute("player");
		playerEndpoint.play();
	}

	@Override
	public void onSessionTerminated(HttpPlayerSession contentSession, int code,
			String reason) throws Exception {
		EventListener.addEvent();
		super.onSessionTerminated(contentSession, code, reason);
	}

	@Override
	public ContentCommandResult onContentCommand(
			HttpPlayerSession contentSession, ContentCommand contentCommand)
			throws Exception {
		EventListener.addEvent();
		return super.onContentCommand(contentSession, contentCommand);
	}

	@Override
	public void onSessionError(HttpPlayerSession contentSession, int code,
			String description) throws Exception {
		EventListener.addEvent();
		super.onSessionError(contentSession, code, description);
	}

	@Override
	public void onUncaughtException(HttpPlayerSession contentSession,
			Throwable exception) throws Exception {
		EventListener.addEvent();
	}

}
