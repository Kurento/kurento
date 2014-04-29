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
 * TODO: review & improve javadoc
 * 
 * Defines the events associated to the RTP operation (
 * {@link #onContentRequest(ContentSession)},
 * {@link #onContentStarted(ContentSession)},
 * {@link #onContentCommand(ContentSession,ContentCommand)},
 * {@link #onUncaughtException(ContentSession,Throwable)},
 * {@link #onSessionTerminated(ContentSession,int,String)}, and
 * {@link #onSessionError(ContentSession,int,String)}); the implementation of
 * the RtpMediaHandler should be used in conjunction with the
 * {@link RtpContentService} annotation. The following snippet shows an skeleton
 * with the implementation of a RtpContentService:
 * 
 * <pre>
 * <code>
 * &#064;RtpContentService(name = &quot;MyRtpHandler&quot;, path = &quot;/my-rtp-media&quot;)
 * public class MyRtpContentHandler implements RtpContentHandler {
 * 
 * 	&#064;Override
 * 	public void onContentRequest(RtpContentSession session) throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onSessionTerminated(RtpContentSession session, int code,String reason) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public onSessionError(RtpContentSession session, int code,String reason) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </code>
 * </pre>
 * 
 * @see RtpContentService
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class RtpContentHandler extends
		ContentHandler<RtpContentSession> {
}
