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
package com.kurento.demo.common;

import com.kurento.demo.cebit.SelectableRoomHandler;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Participant for selectable one to many WebRTC video conference room.
 *
 * @author Miguel París Díaz (mparisdiaz@gmail.com)
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.0.1
 */
public class WebRTCParticipant {

	private String id;
	private String name;

	public final transient WebRtcEndpoint endpoint;
	public final transient WebRtcContentSession session;

	public WebRTCParticipant(String id, String name, WebRtcEndpoint endpoint,
			WebRtcContentSession session) {
		this.name = name;
		this.id = id;
		this.endpoint = endpoint;
		this.session = session;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

}
