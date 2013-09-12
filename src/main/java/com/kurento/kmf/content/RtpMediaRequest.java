package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;

/**
 * 
 * Defines the operations performed by the RtpMediaRequest object, which is in
 * charge of the requesting to a content to be retrieved by RPT from a Media
 * Server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public interface RtpMediaRequest {

	/**
	 * Get the RTP session identifier.
	 * 
	 * @return Session identifier
	 */
	String getSessionId();

	/**
	 * Get the media resource public identification, that can be permanently or
	 * temporarily assigned in the Media Server.
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
	 * Get the operations related with the video stream (SEND, RECV, ...).
	 * 
	 * @return video stream operation
	 */
	public Constraints getVideoConstraints();

	/**
	 * Get the operations related with the audio stream (SEND, RECV, ...).
	 * 
	 * @return audio stream operation
	 */
	public Constraints getAudioConstraints();

	/**
	 * Get the Media Element Pipeline Factory.
	 * 
	 * @return Media Pipeline Factory
	 */
	public MediaPipelineFactory getMediaPipelineFactory();

	/**
	 * Get the Servlet Request, handled by the application server.
	 * 
	 * @return HTTP Servlet Request
	 */
	HttpServletRequest getHttpServletRequest();

	/**
	 * Start the RTP session.
	 * 
	 * @param sinkElement
	 *            In-going media element
	 * @param sourceElement
	 *            Out-going media element
	 * @throws ContentException
	 *             Exception in the RTP process
	 */
	void startMedia(MediaElement sinkElement, MediaElement sourceElement)
			throws ContentException;

	/**
	 * Cancel the operation.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Descriptive message to cancel the operation
	 */
	void reject(int statusCode, String message);
}
