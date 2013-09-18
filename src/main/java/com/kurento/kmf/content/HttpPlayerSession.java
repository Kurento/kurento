package com.kurento.kmf.content;

import com.kurento.kmf.media.MediaElement;
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
public interface HttpPlayerSession extends ContentSession {

	/**
	 * Starts the content exchange for a given content path. TODO: What is the
	 * content path? TODO: Explain what starts mean
	 * 
	 * TODO: IN ALL STARTS OF ALL CONTENTSESSIONS. Explain that if starts throws
	 * and exception, then the session is invalidated. If you don't manage this
	 * exception, it will end-up in onUnmanagedException method of the handler,
	 * but the session will be terminated there.
	 * 
	 * @param contentPath
	 *            Identifies the content in a meaningful way for the Media
	 *            Server
	 * @throws ContentException
	 *             Exception in the strat
	 */
	public void start(String contentPath);

	/**
	 * Starts the content exchange for a given media element. TODO: Explain what
	 * playing a media element means TODO: Explain what starts mean
	 * 
	 * @param source
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the play
	 */
	public void start(MediaElement source);

	/**
	 * Cancel the play operation.
	 * 
	 * @param statusCode
	 *            Error code
	 * @param message
	 *            Descriptive message to cancel the play operation
	 */

	/**
	 * Temporal work-around.
	 * 
	 * @param player
	 *            Player end point
	 */
	public void usePlayer(PlayerEndPoint player);
}
