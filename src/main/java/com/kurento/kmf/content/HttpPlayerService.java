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
 * Annotation for the implementation of a Player; it should be used in
 * conjunction within the implementation of the {@link HttpPlayerHandler}
 * interface. The following snippet shows an skeleton with the implementation of
 * a Player:
 * 
 * <pre>
 * <code>
 * &#064;PlayerService(name = &quot;MyPlayerHandlerName&quot;, path = &quot;/my-player&quot;, redirect = &quot;true&quot;, useControlProtocol = &quot;false&quot;)
 * public class MyPlayerHandler implements PlayerHandler {
 * 
 * 	&#064;Override
 * 	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentPlayed(PlayRequest playRequest) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentError(PlayRequest playRequest,
 * 			ContentException exception) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </code>
 * </pre>
 * 
 * @see HttpPlayerHandler
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpPlayerService {

	/**
	 * Name of the PlayerHandler; this name MUST be unique, in other words, in
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

	/**
	 * Activates a JSON signaling protocol; this protocol is used for media
	 * negotiation.
	 */
	boolean useControlProtocol() default true;

	/**
	 * If true, this parameters instructs that content must be served from the
	 * Media Server straight to the client, but it could also be proxied through
	 * the Application Server by setting this parameter to false.
	 */
	boolean redirect() default false;
}
