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

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;

@HttpPlayerService(name = "PlayerHttpJsonHandler", path = "/playerJson/*", useControlProtocol = true)
public class PlayerHttpJsonHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		getLogger().info("Received play request to " + session.getContentId());

		String fileUri = "";
		if (session.getContentId() != null
				&& session.getContentId().toLowerCase().startsWith("b")) {
			fileUri = "file:///opt/video/barcodes.webm";
		} else if (session.getContentId() != null
				&& session.getContentId().toLowerCase().startsWith("f")) {
			fileUri = "file:///opt/video/fiwarecut.webm";
		} else {
			fileUri = "file:///opt/video/sintel.webm";
		}

		getLogger().info("playRequest.play( " + fileUri + ")");
		session.start(fileUri);
	}

}
