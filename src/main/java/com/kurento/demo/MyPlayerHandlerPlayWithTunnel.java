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
package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "MyPlayerHandlerPlayWithTunnel", path = "/player-play-with-tunnel", redirect = false)
public class MyPlayerHandlerPlayWithTunnel implements PlayerHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyPlayerHandlerPlayWithTunnel.class);

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		log.debug("onPlayRequest");
		playRequest.play("http://ci.kurento.com/downloads/small.webm");
	}

	@Override
	public void onContentPlayed(PlayRequest playRequest) {
		log.debug("onContentPlayed");
	}

	@Override
	public void onContentError(PlayRequest playRequest,
			ContentException exception) {
		log.debug("onContentError " + exception.getMessage());
	}
}
