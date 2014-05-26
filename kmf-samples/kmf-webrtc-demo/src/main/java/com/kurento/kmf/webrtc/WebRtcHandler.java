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
package com.kurento.kmf.webrtc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;
import com.kurento.kmf.jsonrpcconnector.DefaultJsonRpcHandler;
import com.kurento.kmf.jsonrpcconnector.Session;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * WebRTC handler (application logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class WebRtcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private final Logger log = LoggerFactory.getLogger(WebRtcHandler.class);

	@Autowired
	private MediaPipelineFactory mpf;

	private Session session;

	@Override
	public void afterConnectionEstablished(Session session) throws Exception {
		this.session = session;
	}

	@Override
	public void handleRequest(Transaction transaction,
			Request<JsonObject> request) throws Exception {
		JsonObject params = request.getParams();
		switch (request.getMethod()) {
		case "start":
			call(transaction, params);
			break;
		default:
			break;
		}
	}

	private void call(Transaction transaction, JsonObject params)
			throws IOException {
		// SDP Offer
		String sdpOffer = params.get("sdpOffer").getAsString();
		log.debug("Received SDP offer");

		// Media Logic
		MediaPipeline mp = mpf.create();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		FaceOverlayFilter faceOverlayFilter = mp.newFaceOverlayFilter().build();
		faceOverlayFilter.setOverlayedImage(
				"http://ci.kurento.com/imgs/mario-wings.png", -0.35F, -1.2F,
				1.6F, 1.6F);
		webRtcEndpoint.connect(faceOverlayFilter);
		faceOverlayFilter.connect(webRtcEndpoint);

		// SDP Answer
		String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
		JsonObject scParams = new JsonObject();
		scParams.addProperty("sdpAnswer", sdpAnswer);
		session.sendRequest("started", scParams);
	}

}
