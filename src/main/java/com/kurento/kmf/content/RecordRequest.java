package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;

/**
 * 
 * Defines the operations performed by the RecordRequest object, which is in
 * charge of the recording a content in a Media Server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface RecordRequest {

	/**
	 * Get the media resource public identification, that can be permanently or
	 * temporarily assigned in the Media Server.
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
	 * Get the Servlet Request, handled by the application server.
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
	 * Performs a record action of a given content path.
	 * 
	 * @param contentPath
	 *            Identifies the content in a meaningful way for the Media
	 *            Server
	 * @throws ContentException
	 *             Exception in the record
	 */
	public void record(String contentPath) throws ContentException;

	/**
	 * Performs a record action of media elements.
	 * 
	 * @param element
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the record
	 */
	public void record(MediaElement element) throws ContentException;

	/**
	 * Cancel the record operation.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Descriptive message to cancel the record operation
	 */
	public void reject(int statusCode, String message);
}
