package com.kurento.kmf.content;

import javax.servlet.http.HttpServletRequest;

import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndPoint;

/**
 * Defines the operations performed by the PlayRequest object, which is in
 * charge of the requesting to a content to be retrieved from a Media Server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface PlayRequest {

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
	 * Performs a play action of a given content path.
	 * 
	 * @param contentPath
	 *            Identifies the content in a meaningful way for the Media
	 *            Server
	 * @throws ContentException
	 *             Exception in the play
	 */
	public void play(String contentPath) throws ContentException;

	/**
	 * Temporal work-around.
	 * 
	 * @param player
	 *            Player end point
	 */
	public void usePlayer(PlayerEndPoint player);

	/**
	 * Performs a play action of media elements.
	 * 
	 * @param source
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the play
	 */
	public void play(MediaElement source) throws ContentException;

	/**
	 * Cancel the play operation.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Descriptive message to cancel the play operation
	 */
	public void reject(int statusCode, String message);
}
