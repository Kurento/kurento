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
package com.kurento.demo.webrtc;

import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.WebRtcContentSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaProfileSpecType;
import com.kurento.kmf.media.RecorderEndpoint;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * WebRtc Handler in loopback connected to a RecorderEndpoint.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
@WebRtcContentService(path = "/webRtcRecorderLoopback/*")
public class WebRtcRecorder extends WebRtcContentHandler {

	public static final String TARGET = "file:///tmp/webrtc";

	private RecorderEndpoint recorderEndPoint;

	@Override
	public void onContentRequest(WebRtcContentSession contentSession)
			throws Exception {
		MediaPipeline mp = contentSession.getMediaPipelineFactory().create();
		contentSession.releaseOnTerminate(mp);

		// By default recording in WEBM format
		MediaProfileSpecType mediaProfileSpecType = MediaProfileSpecType.WEBM;
		final String contentId = contentSession.getContentId();
		if (contentId != null && contentId.equalsIgnoreCase("mp4")) {
			mediaProfileSpecType = MediaProfileSpecType.MP4;
		}
		recorderEndPoint = mp.newRecorderEndpoint(TARGET)
				.withMediaProfile(mediaProfileSpecType).build();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();

		webRtcEndpoint.connect(webRtcEndpoint);
		webRtcEndpoint.connect(recorderEndPoint);
		contentSession.start(webRtcEndpoint);
	}

	@Override
	public void onContentStarted(WebRtcContentSession contentSession) {
		recorderEndPoint.record();
	}

	@Override
	public void onSessionTerminated(WebRtcContentSession contentSession,
			int code, String reason) throws Exception {
		recorderEndPoint.stop();
		recorderEndPoint.release();
		super.onSessionTerminated(contentSession, code, reason);
	}
}