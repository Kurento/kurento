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
/**
 *
 * This package contains the Kurento Media Framework Content API.
 * The Kurento Content API (javadoc) is a Java EE layer which consumes
 * the <a href="http://www.kurento.org/docs/current/kmf-media-api/packages.html">
 * Kurento Media API</a> and exposes its capabilities through two types of objects:
 * ContentHandlers and ContentSessions. Additionally service annotations
 * allow the simple deployment of the ContentHandlers as services.
 *
 * The following snippet shows an skeleton with the implementation of
 * a {@link com.kurento.kmf.content.ContentHandler ContentHandler}
 * exposed as a {@link com.kurento.kmf.content.HttpRecorderService}:
 *
 * <pre><code>
 * &#064;RecorderService(name = &quot;MyRecorderHandlerName&quot;,
 *                  path = &quot;/my-recorder&quot;,
 *                  redirect = &quot;true&quot;,
 *                  useControlProtocol = "false&quot;)
 * public class MyRecorderHandlerRecord implements RecorderHandler {
 *
 * 	&#064;Override
 * 	public void onRecordRequest(RecordRequest recordRequest)
 * 			throws ContentException {
 * 		// My implementation
 * 	}
 *
 * 	&#064;Override
 * 	public void onContentRecorded(String contentId) {
 * 		// My implementation
 * 	}
 *
 * 	&#064;Override
 * 	public void onContentError(String contentId, ContentException exception) {
 * 		// My implementation
 * 	}
 * }
 * </code></pre>
 *
 * @see <a href="http://www.kurento.org/documentation">Kurento Documentation</a>
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
package com.kurento.kmf.content;

