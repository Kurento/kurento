/*
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
package com.kurento.demo.mixer;

import static java.lang.reflect.Modifier.TRANSIENT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kurento.demo.webrtc.DispatcherParticipant;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.Composite;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * WebRtc Dispatcher many to many with composite (mixer).
 * 
 * @author Miguel París Díaz (mparisdiaz@gmail.com)
 * @author Santiago Carot (sancane.kurento@gmail.com)
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.1
 */
@WebRtcContentService(path = "/compositeMixerWebRTC/*")
public class CompositeMixerWebRTC extends WebRtcContentHandler {

	/* Commands */
	public static final String COMMAND_GET_PARTICIPANTS = "getParticipants";
	public static final String COMMAND_SELECT = "selectParticipant";
	public static final String COMMAND_CONNECT = "connectParticipant";

	/* Events */
	public static final String EVENT_ON_JOINED = "onJoined";
	public static final String EVENT_ON_UNJOINED = "onUnjoined";

	/* Global variables */
	private MediaPipeline mp;
	private Composite compsite;
	private Map<String, DispatcherParticipant> participants;
	private static final Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(TRANSIENT).create();

	@Override
	public void onContentRequest(WebRtcContentSession session) throws Exception {
		if (mp == null) {
			synchronized (this) {
				if (mp == null) {
					mp = session.getMediaPipelineFactory().create();
					compsite = mp.newComposite().build();
					participants = new ConcurrentHashMap<String, DispatcherParticipant>();
				}
			}
		}
		String name = session.getContentId();
		if (name == null || name.isEmpty()) {
			name = "<no name>";
		}
		if (existName(name)) {
			session.terminate(403, "User " + name + " is already in the room. "
					+ "Please select another name and try again.");
		} else {
			WebRtcEndpoint endpoint = mp.newWebRtcEndpoint().build();
			HubPort hubPort = compsite.newHubPort().build();
			endpoint.connect(hubPort);
			hubPort.connect(endpoint);
			DispatcherParticipant participant = new DispatcherParticipant(
					session.getSessionId(), name, endpoint, session, hubPort);
			session.start(participant.endpoint);
			session.setAttribute("participant", participant);
			participants.put(participant.getId(), participant);
			notifyJoined(participant);
			hubPort.connect(endpoint);
		}
	}

	@Override
	public ContentCommandResult onContentCommand(WebRtcContentSession session,
			ContentCommand command) throws Exception {
		String cmdType = command.getType();
		String cmdData = command.getData();
		getLogger().info("onContentCommand: ({}, {})", cmdType, cmdData);

		if (COMMAND_GET_PARTICIPANTS.equalsIgnoreCase(cmdType)) {
			String json = gson.toJson(participants.values());
			return new ContentCommandResult(json);
		} else if (COMMAND_SELECT.equalsIgnoreCase(cmdType)) {
			return new ContentCommandResult(Boolean.toString(selectParticipant(
					session, cmdData)));
		}
		return super.onContentCommand(session, command);
	}

	@Override
	public synchronized void onSessionTerminated(WebRtcContentSession session,
			int code, String reason) throws Exception {
		DispatcherParticipant participant = (DispatcherParticipant) session
				.getAttribute("participant");
		participants.remove(participant.getId());

		getLogger().info("---> remove port");
		getLogger().info("---> number participants {}", participants.size());
		getLogger().info("---> participant name {}", participant.getId());
		try {
			participant.port.release();
			participant.endpoint.release();
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().info("Catched");
		}

		if (participants.size() == 0) {
			getLogger().info("---> clearing room");
			mp.release();
			mp = null;
			participants.clear();
		} else {
			getLogger()
					.info("---> number participants {}", participants.size());
			notifyUnjoined(participant);
		}
		super.onSessionTerminated(session, code, reason);
	}

	private boolean selectParticipant(WebRtcContentSession session,
			String partId) {
		return true;
	}

	private boolean existName(final String name) {
		for (DispatcherParticipant p : participants.values()) {
			if (p.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	private void notifyJoined(DispatcherParticipant participant) {
		String json = gson.toJson(participant);
		getLogger().info("Participant joined: {}", json);
		for (DispatcherParticipant p : participants.values()) {
			p.session.publishEvent(new ContentEvent(EVENT_ON_JOINED, json));
		}
	}

	private void notifyUnjoined(DispatcherParticipant participant) {
		String json = gson.toJson(participant);
		getLogger().info("Participant unjoined: {}", json);
		for (DispatcherParticipant p : participants.values()) {
			p.session.publishEvent(new ContentEvent(EVENT_ON_UNJOINED, json));
		}
	}

}
