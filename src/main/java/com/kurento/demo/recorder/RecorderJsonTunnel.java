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
package com.kurento.demo.recorder;

import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.HttpRecorderService;
import com.kurento.kmf.content.HttpRecorderSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndpoint;

/**
 * HTTP Recorder Handler; tunnel strategy (redirect=false, by default); using
 * JSON-RPC control protocol (useControlProtocol=true, by default).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpRecorderService(path = "/recorderJsonTunnel")
public class RecorderJsonTunnel extends HttpRecorderHandler {

	@Override
	public void onContentRequest(HttpRecorderSession contentSession)
			throws Exception {
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);
		RecorderEndpoint recorderEndPoint = mp.newRecorderEndpoint(
				"file:///tmp/recorderJsonTunnel").build();
		contentSession.setAttribute("recorder", recorderEndPoint);
		contentSession.start(recorderEndPoint);
	}

	@Override
	public void onContentStarted(HttpRecorderSession contentSession) {
		RecorderEndpoint recorderEndPoint = (RecorderEndpoint) contentSession
				.getAttribute("recorder");
		recorderEndPoint.record();
	}

	@Override
	public void onSessionTerminated(HttpRecorderSession contentSession,
			int code, String reason) throws Exception {
		RecorderEndpoint recorderEndPoint = (RecorderEndpoint) contentSession
				.getAttribute("recorder");
		recorderEndPoint.stop();
		super.onSessionTerminated(contentSession, code, reason);
	}
}
