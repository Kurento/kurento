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

import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Receiver in a chat room; this class is composed by the WebRTC element, the
 * content session (needed to send events with the nick name when connections
 * are made) and boolean value setting wheter or not the connection has been
 * established.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 3.0.7
 */
public class Receiver {

	private WebRtcEndpoint webRtcEndpoint;

	private ContentSession contentSession;

	private boolean connected;

	public Receiver(WebRtcEndpoint webRtcEndpoint, ContentSession contentSession) {
		this.webRtcEndpoint = webRtcEndpoint;
		this.contentSession = contentSession;
		connected = false;
	}

	public WebRtcEndpoint getWebRtcEndpoint() {
		return webRtcEndpoint;
	}

	public ContentSession getContentSession() {
		return contentSession;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

}
