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

package org.kurento.kmf.composite;

import org.kurento.kmf.media.HubPort;
import org.kurento.kmf.media.WebRtcEndpoint;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author David Fernández-López (d.fernandezlop@gmail.com)
 * 
 */

public class Participant {
	private final WebRtcEndpoint w;
	private final HubPort h;

	public Participant(WebRtcEndpoint w, HubPort h) {
		this.h = h;
		this.w = w;
	}

	public WebRtcEndpoint getEndpoint() {
		return w;
	}

	public HubPort getPort() {
		return h;
	}
}
