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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: review & improve javadoc Anotation for the implementation of a
 * RtpContentHandler; it should be used in conjunction within the implementation
 * of the {@link RtpContentHandler} interface. The following snippet shows an
 * skeleton with the implementation of a RTP Handler:
 * 
 * <pre>
 * <code>
 * &#064;RtpContentService(name = &quot;MyHandler&quot;, path = &quot;/my-webrtc-media&quot;)
 * public class MyHandler implements WebRtcContentHandler {
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
 * 	public onSessionError(WebRContentSession session, int code, String description) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </code>
 * </pre>
 * 
 * @see WebRtcContentHandler
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebRtcContentService {
	/**
	 * Name of the WebRTC Handler; this name MUST be unique; in other words, in
	 * several handlers exists within the same application, each of them must
	 * have a different name.
	 * 
	 */
	String name() default "";

	/**
	 * The handler will be instrumented as a HTTP Servlet in the application
	 * server; this parameter establishes the path of the servlet; the same way
	 * as the name, if several handlers co-exists within the same application,
	 * the paths must be also different.
	 */
	String path();
}
