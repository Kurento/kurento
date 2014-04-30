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

import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.PlayerEndpoint;

/**
 * HTTP Player Handler; tunnel strategy; no JSON control protocol.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpPlayerService(path = "/playerTunnel/*", redirect = false, useControlProtocol = false)
public class PlayerTunnel extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession contentSession)
			throws Exception {
		GenericPlayer.play(contentSession);
	}

	@Override
	public void onContentStarted(HttpPlayerSession contentSession)
			throws Exception {
		if (contentSession.getAttribute("player") != null) {
			PlayerEndpoint playerEndpoint = (PlayerEndpoint) contentSession
					.getAttribute("player");
			playerEndpoint.play();
		}
		super.onContentStarted(contentSession);
	}

	@Override
	public void onSessionTerminated(HttpPlayerSession contentSession, int code,
			String reason) throws Exception {
		super.onSessionTerminated(contentSession, code, reason);
	}

	@Override
	public ContentCommandResult onContentCommand(
			HttpPlayerSession contentSession, ContentCommand contentCommand)
			throws Exception {
		return super.onContentCommand(contentSession, contentCommand);
	}

	@Override
	public void onSessionError(HttpPlayerSession contentSession, int code,
			String description) throws Exception {
		super.onSessionError(contentSession, code, description);
	}

	@Override
	public void onUncaughtException(HttpPlayerSession contentSession,
			Throwable exception) throws Exception {
		super.onUncaughtException(contentSession, exception);
	}

}
