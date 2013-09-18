package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipelineFactory;

/**
 * 
 * Defines the operations performed by a request to the Media Server.
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
	public String getSessionId();

	/**
	 * Get the media resource public identification, that can be permanently or
	 * temporarily assigned in the Media Server.
	 * 
	 * TODO: explain how it is extracted from path
	 * 
	 * @return Media identification
	 */
	public String getContentId();

	/**
	 * Media attribute accessor (getter).
	 * 
	 * @param name
	 *            Name of the attribute
	 * @return Media Attribute
	 */
	public Object getAttribute(String name);

	/**
	 * Media attribute mutator (setter).
	 * 
	 * @param name
	 *            Name of the attribute
	 * @param value
	 *            Value of the attribute
	 * @return Value of the set attribute
	 */
	public Object setAttribute(String name, Object value);

	/**
	 * Media attribute eraser.
	 * 
	 * @param name
	 *            Name of the attribute
	 * @return Value of the deleted attribute
	 */
	public Object removeAttribute(String name);

	/**
	 * Get the Servlet Request, handled by the application server. Given that
	 * the lifecyle of the HttpServlerRequest is independent from the one of the
	 * ContentSession, this method can only be invoked in the context of the
	 * "onContentRequest" of the ContentHandler
	 * 
	 * @return HTTP Servlet Request
	 */
	public HttpServletRequest getHttpServletRequest();

	/**
	 * Media Pipeline Factory accessor (getter).
	 * 
	 * @return Media Pipeline Factory
	 */
	public MediaPipelineFactory getMediaPipelineFactory();

	/**
	 * Terminates this session. The session cannot be used after invoking this
	 * method. Only session attributes will be available.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Descriptive message to cancel the record operation
	 */
	public void terminate(int statusCode, String message);

	/**
	 * Publishes a media event to the client. This method only makes sense when
	 * the useControlProtocol flag is set to true in the annotation service of
	 * the Handler
	 * 
	 * @param event
	 *            the event to be published
	 */
	public void publishEvent(ContentEvent contentEvent);

	/**
	 * Guarantees that a given media object is released (invoking its release
	 * method) when this session terminates either with or without error
	 * 
	 * @param mediaObject
	 */
	public void releaseOnTerminate(MediaObject mediaObject);
}