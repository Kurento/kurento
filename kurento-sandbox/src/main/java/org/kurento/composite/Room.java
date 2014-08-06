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
package org.kurento.composite;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.kurento.jsonrpcconnector.Session;
import org.kurento.media.Composite;
import org.kurento.media.HubPort;
import org.kurento.media.MediaPipeline;
import org.kurento.media.WebRtcEndpoint;
import org.kurento.media.factory.MediaPipelineFactory;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author David Fernández-López (d.fernandezlop@gmail.com)
 * 
 */
public class Room {
	// private final Logger log = LoggerFactory.getLogger(Room.class);
	@Autowired
	private MediaPipelineFactory mpf;

	private MediaPipeline mp;
	private Composite composite;
	private final ConcurrentMap<String, Participant> participants = new ConcurrentHashMap<>();

	@PostConstruct
	private void init() {
		mp = mpf.create();
		composite = mp.newComposite().build();
	}

	public MediaPipeline getPipeline() {
		return mp;
	}

	public Composite getComposite() {
		return composite;
	}

	public void joinParticipant(Session session, WebRtcEndpoint w, HubPort h)
			throws IOException {
		Participant participant = new Participant(w, h);
		participants.put(session.getSessionId(), participant);
		return;
	}

	public void removeParticipant(Session session) {

		Participant removedParticipant = participants.remove(session
				.getSessionId());

		removedParticipant.getPort().release();
		removedParticipant.getEndpoint().release();
	}
}
