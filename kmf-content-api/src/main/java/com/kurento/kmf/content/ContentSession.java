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

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.factory.MediaPipelineFactory;
import com.kurento.kmf.repository.Repository;

/**
 * 
 * Defines the operations performed by a media request to the Media Server.
 * 
 * A ContentSession handles a session for media exchange between a client and a
 * Media Server. Its typical lifetime encompasses a number of transport protocol
 * requests (for request-response protocols such as HTTP) and the exchange of a
 * number of commands and events.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface ContentSession {

	/**
	 * 
	 * @return unique Id of this session
	 */
	String getSessionId();

	/**
	 * Get the media resource public identification, that can be permanently or
	 * temporarily assigned in the Media Server.
	 * 
	 * TODO: explain how it is extracted from path
	 * 
	 * @return Media identification
	 */
	String getContentId();

	/**
	 * Media attribute accessor (getter).
	 * 
	 * @param name
	 *            Name of the attribute
	 * @return Media Attribute
	 */
	Object getAttribute(String name);

	/**
	 * Media attribute mutator (setter).
	 * 
	 * @param name
	 *            Name of the attribute
	 * @param value
	 *            Value of the attribute
	 * @return Value of the set attribute
	 */
	Object setAttribute(String name, Object value);

	/**
	 * Media attribute eraser.
	 * 
	 * @param name
	 *            Name of the attribute
	 * @return Value of the deleted attribute
	 */
	Object removeAttribute(String name);

	/**
	 * Get the Servlet Request, handled by the application server. Given that
	 * the lifecyle of the HttpServlerRequest is independent from the one of the
	 * ContentSession, this method can only be invoked in the context of the
	 * "onContentRequest" of the ContentHandler
	 * 
	 * @return HTTP Servlet Request
	 */
	HttpServletRequest getHttpServletRequest();

	/**
	 * Media Pipeline Factory accessor (getter).
	 * 
	 * @return Media Pipeline Factory
	 */
	MediaPipelineFactory getMediaPipelineFactory();

	/**
	 * Terminates this session. The session cannot be used after invoking this
	 * method. Only session attributes will be available.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Descriptive message to cancel the record operation
	 */
	void terminate(int statusCode, String message);

	/**
	 * Publishes a media event to the client. This method only makes sense when
	 * the useControlProtocol flag is set to true in the annotation service of
	 * the Handler
	 * 
	 * @param contentEvent
	 *            the event to be published
	 */
	void publishEvent(ContentEvent contentEvent);

	/**
	 * Guarantees that a given media object is released (invoking its release
	 * method) when this session terminates either with or without error
	 * 
	 * @param mediaObject
	 */
	void releaseOnTerminate(MediaObject mediaObject);

	/**
	 * Gets the {@link com.kurento.kmf.repository.Repository} associated with
	 * this ContentSession.
	 * 
	 * @return a Media Repository
	 */
	Repository getRepository();

}
