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

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

@HttpPlayerService(name = "CpRtcPlayerZbarHandler", path = "/cpRtcPlayerZbar", redirect = true, useControlProtocol = true)
public class CpRtcPlayerZbarHandler extends HttpPlayerHandler {

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {
		if (CpRtcRtpZbarHandler.sharedFilterReference == null) {
			session.terminate(500, "Rtp session has not been established");
			return;
		}
		CpRtcRtpZbarHandler.sharedFilterReference
				.addCodeFoundListener(new MediaEventListener<CodeFoundEvent>() {

					@Override
					public void onEvent(CodeFoundEvent event) {
						session.publishEvent(new ContentEvent(event.getType(),
								event.getValue()));
					}
				});

		HttpGetEndpoint httpEndpoint = CpRtcRtpZbarHandler.sharedFilterReference
				.getMediaPipeline().newHttpGetEndpoint().terminateOnEOS()
				.build();
		CpRtcRtpZbarHandler.sharedFilterReference.connect(httpEndpoint);
		session.start(httpEndpoint);
	}

}
