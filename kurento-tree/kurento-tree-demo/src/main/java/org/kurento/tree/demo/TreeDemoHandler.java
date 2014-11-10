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
package org.kurento.tree.demo;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Protocol handler for 1 to N video call communication.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class TreeDemoHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(TreeDemoHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	private ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<String, UserSession>();

	@Autowired
	private KurentoTreeClient kurentoTree;

	private UserSession masterUserSession;

	private String treeId;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(),
				JsonObject.class);
		log.debug("Incoming message from session '{}': {}", session.getId(),
				jsonMessage);

		switch (jsonMessage.get("id").getAsString()) {
		case "master":
			try {
				master(session, jsonMessage);
			} catch (Throwable t) {
				stop(session);
				log.error(t.getMessage(), t);
				JsonObject response = new JsonObject();
				response.addProperty("id", "masterResponse");
				response.addProperty("response", "rejected");
				response.addProperty("message", t.getMessage());
				session.sendMessage(new TextMessage(response.toString()));
			}
			break;
		case "viewer":
			try {
				viewer(session, jsonMessage);
			} catch (Throwable t) {
				stop(session);
				log.error(t.getMessage(), t);
				JsonObject response = new JsonObject();
				response.addProperty("id", "viewerResponse");
				response.addProperty("response", "rejected");
				response.addProperty("message", t.getMessage());
				session.sendMessage(new TextMessage(response.toString()));
			}
			break;
		case "stop":
			stop(session);
			break;
		default:
			break;
		}
	}

	private synchronized void master(WebSocketSession session,
			JsonObject jsonMessage) throws IOException {

		if (masterUserSession == null) {
			masterUserSession = new UserSession(session);

			treeId = kurentoTree.createTree();

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer")
					.getAsString();

			String sdpAnswer = kurentoTree.setTreeSource(treeId, sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "masterResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);
			masterUserSession.sendMessage(response);

		} else {

			JsonObject response = new JsonObject();
			response.addProperty("id", "masterResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message",
					"Another user is currently acting as sender. Try again later ...");
			session.sendMessage(new TextMessage(response.toString()));
		}
	}

	private synchronized void viewer(WebSocketSession session,
			JsonObject jsonMessage) throws IOException {

		if (masterUserSession == null) {

			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message",
					"No active sender now. Become sender or . Try again later ...");
			session.sendMessage(new TextMessage(response.toString()));

		} else {

			if (viewers.containsKey(session.getId())) {
				JsonObject response = new JsonObject();
				response.addProperty("id", "viewerResponse");
				response.addProperty("response", "rejected");
				response.addProperty(
						"message",
						"You are already viewing in this session. Use a different browser to add additional viewers.");
				session.sendMessage(new TextMessage(response.toString()));
				return;
			}

			UserSession viewer = new UserSession(session);
			viewers.put(session.getId(), viewer);

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer")
					.getAsString();

			TreeEndpoint treeEndpoint = kurentoTree.addTreeSink(treeId,
					sdpOffer);

			String sdpAnswer = treeEndpoint.getSdp();

			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);
			viewer.sendMessage(response);
		}
	}

	private synchronized void stop(WebSocketSession session) throws IOException {
		String sessionId = session.getId();
		if (masterUserSession != null
				&& masterUserSession.getSession().getId().equals(sessionId)) {

			for (UserSession viewer : viewers.values()) {
				JsonObject response = new JsonObject();
				response.addProperty("id", "stopCommunication");
				viewer.sendMessage(response);
			}

			log.info("Releasing media pipeline");
			kurentoTree.releaseTree(treeId);
			treeId = null;
			masterUserSession = null;

		} else if (viewers.containsKey(sessionId)) {

			String sinkId = viewers.get(sessionId).getSinkId();
			if (sinkId != null) {
				kurentoTree.removeTreeSink(treeId, sinkId);
			}
			viewers.remove(sessionId);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {
		stop(session);
	}

}
