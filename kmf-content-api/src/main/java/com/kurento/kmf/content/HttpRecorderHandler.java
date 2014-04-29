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
 * Defines the events associated to the record operation (
 * {@link #onContentRequest(ContentSession)},
 * {@link #onSessionTerminated(ContentSession,int,String)}, and
 * {@link #onSessionError(ContentSession,int,String)}); the implementation of
 * the HttpRecorderHandler should be used in conjunction with
 * {@link HttpRecorderService} annotation. The following snippet shows an
 * skeleton with the implementation of a Recorder:
 * 
 * <pre>
 * <code>
 * &#064;RecorderService(name = &quot;MyHandlerName&quot;,
 *                       path = &quot;/my-recorder&quot;,
 *                       redirect = &quot;true&quot;,
 *                       useControlProtocol = &quot;false&quot;)
 * public class MyRecorderHandlerRecord implements ContentHandler<> {
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
 * </code>
 * </pre>
 * 
 * @see HttpRecorderService
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public abstract class HttpRecorderHandler extends
		ContentHandler<HttpRecorderSession> {
}
