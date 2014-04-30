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
package com.kurento.demo.webrtc.chat;

import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Transmitter in a chat room; this class is composed by the WebRTC element and
 * the nick name.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 3.0.7
 */
public class Transmitter {

	private WebRtcEndpoint webRtcEndpoint;

	private String nick;

	public Transmitter(WebRtcEndpoint webRtcEndpoint, String nick) {
		this.webRtcEndpoint = webRtcEndpoint;
		this.nick = nick;
	}

	public WebRtcEndpoint getWebRtcEndpoint() {
		return webRtcEndpoint;
	}

	public String getNick() {
		return nick;
	}

}
