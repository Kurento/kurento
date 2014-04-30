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
package com.kurento.demo.cplondon;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;

@HttpPlayerService(name = "CpRtcPlayerHandler", path = "/cpRtcPlayerJack", redirect = true, useControlProtocol = true)
public class CpRtcPlayerJackHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		if (CpRtcRtpJackHandler.sharedFilterReference == null) {
			session.terminate(500, "Rtp session has not been established");
		} else {
			HttpGetEndpoint httpEndpoint = CpRtcRtpJackHandler.sharedFilterReference
					.getMediaPipeline().newHttpGetEndpoint().terminateOnEOS()
					.build();
			CpRtcRtpJackHandler.sharedFilterReference.connect(httpEndpoint);
			session.start(httpEndpoint);
		}
	}

}
