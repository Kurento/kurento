package com.kurento.kmf.content;

import com.kurento.kmf.content.jsonrpc.Constraints;
import com.kurento.kmf.media.MediaElement;

/**
 * TODO: write javadoc
 * 
 * @author llopez
 * 
 */
public interface SdpContentSession extends ContentSession {
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
	 * Start the RTP session.
	 * 
	 * @param sinkElement
	 *            In-going media element
	 * @param sourceElement
	 *            Out-going media element
	 * @throws ContentException
	 *             Exception in the RTP process
	 */
	void start(MediaElement sourceElement, MediaElement... sinkElements);

	// TODO: javadoc
	void start(MediaElement sourceElement, MediaElement sinkElement);
}
