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
package com.kurento.kmf.content;

/**
 * 
 * Defines the events associated to the WebRTC operation (
 * {@link #onContentRequest(ContentSession)},
 * {@link #onContentStarted(ContentSession)},
 * {@link #onContentCommand(ContentSession,ContentCommand)},
 * {@link #onUncaughtException(ContentSession,java.lang.Throwable)},
 * {@link #onSessionTerminated(ContentSession,int,java.lang.String)}, and
 * {@link #onSessionError(ContentSession,int,java.lang.String)}); the
 * implementation of the RtpMediaHandler should be used in conjunction with the
 * {@link WebRtcContentService} annotation. The following snippet shows an
 * skeleton with the implementation of a WebRTC Handler:
 * 
 * <pre>
 * <code>
 * &#064;WebRtcContentService(name = &quot;MyRtpHandler&quot;, path = &quot;/my-rtp-media&quot;)
 * public class MyRtpMediaHandler implements RtpMediaHandler {
 * 
 * 	&#064;Override
 * 	public void onContentRequest(WebRtcContentSession session) throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onSessionTerminated(WebRtcContentSession session, int code, String reason) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public onSessionError(WebRtcContentSession session, int code, String reason) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </code>
 * </pre>
 * 
 * @see WebRtcContentService
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class WebRtcContentHandler extends
		ContentHandler<WebRtcContentSession> {
}
