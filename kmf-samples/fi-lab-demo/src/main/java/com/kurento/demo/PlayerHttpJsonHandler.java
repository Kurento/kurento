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

/**
 * HTTP Player Handler which plays a collection of videos depending on the
 * <code>contentId</code> of the request; using tunneling strategy (by default
 * <code>redirect=true</code> in {@link HttpPlayerService} annotation; with JSON
 * signalling protocol.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see VideoURLs
 */
@HttpPlayerService(name = "PlayerHttpJsonHandler", path = "/playerJson/*", useControlProtocol = true, redirect = true)
public class PlayerHttpJsonHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		// MP4 video by default
		String url = VideoURLs.map.get("mp4");

		// Depending of contentId, we can play a different video
		String contentId = session.getContentId();
		if (contentId != null && VideoURLs.map.containsKey(contentId)) {
			url = VideoURLs.map.get(contentId);
		}
		getLogger().info("Playing " + url);
		session.start(url);
	}

}
