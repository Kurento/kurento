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
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaRequest;
import com.kurento.kmf.content.WebRtcMediaService;
import com.kurento.kmf.content.internal.webrtc.WebRtcMediaRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcEvent;

@WebRtcMediaService(name = "WebRtcMediaHandler", path = "/webrtc")
public class MyWebRtcMediaHandler implements WebRtcMediaHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyWebRtcMediaHandler.class);

	@Override
	public void onMediaRequest(WebRtcMediaRequest request)
			throws ContentException {
		log.debug("onMediaRequest");
		request.startMedia(null, null);
		((WebRtcMediaRequestImpl) request).produceEvents(JsonRpcEvent.newEvent(
				"test-event-type", "test-event-data"));
	}

	@Override
	public void onMediaTerminated(String requestId) {
		log.debug("onMediaTerminated");
	}

	@Override
	public void onMediaError(String requestId, ContentException exception) {
		log.debug("onMediaError");
	}
}
