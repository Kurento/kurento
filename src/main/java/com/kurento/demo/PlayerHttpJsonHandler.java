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

@PlayerService(name = "PlayerHttpJsonHandler", path = "/playerJson/*", useControlProtocol = true)
public class PlayerHttpJsonHandler implements PlayerHandler {

	private static final Logger log = LoggerFactory
			.getLogger(PlayerHttpHandler.class);

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		log.info("Received play request to " + playRequest.getContentId());

		String fileUri = "";
		if (playRequest.getContentId() != null
				&& playRequest.getContentId().toLowerCase().startsWith("b")) {
			fileUri = "file:///opt/video/barcodes.webm";
		}
		if (playRequest.getContentId() != null
				&& playRequest.getContentId().toLowerCase().startsWith("f")) {
			fileUri = "file:///opt/video/fiwarecut.webm";
		} else {
			fileUri = "file:///opt/video/sintel.webm";
		}

		log.info("playRequest.play( " + fileUri + ")");
		playRequest.play(fileUri);
	}

	@Override
	public void onContentPlayed(PlayRequest playRequest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContentError(PlayRequest playRequest,
			ContentException exception) {
		// TODO Auto-generated method stub

	}

}
