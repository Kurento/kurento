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

import com.kurento.demo.common.WebRTCParticipant;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.HubPort;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Participant for selectable one to many WebRTC video conference room.
 *
 * @author Santiago Carot Nemesio sancane.kurento@gmail.com
 */

public class DispatcherParticipant extends WebRTCParticipant {

	public final transient HubPort port;

	public DispatcherParticipant(String id, String name, WebRtcEndpoint endpoint,
			WebRtcContentSession session, HubPort port) {
		super(id, name, endpoint, session);
		this.port = port;
	}

}
