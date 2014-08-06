/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media;

import org.kurento.tool.rom.RemoteClass;
import org.kurento.tool.rom.server.Param;

/**
 * 
 * Special type of pad, used by a {@link MediaElement} to receive a media
 * stream.
 * 
 **/
@RemoteClass
public interface MediaSink extends MediaPad {

	/**
	 * 
	 * Disconnects the current sink from the referred {@link MediaSource}
	 * 
	 * @param src
	 *            The source to disconnect
	 * 
	 **/
	void disconnect(@Param("src") MediaSource src);

	/**
	 * 
	 * Asynchronous version of disconnect: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaSink#disconnect
	 * 
	 * @param src
	 *            The source to disconnect
	 * 
	 **/
	void disconnect(@Param("src") MediaSource src, Continuation<Void> cont);

	/**
	 * 
	 * Gets the {@link MediaSource} that is connected to this sink.
	 * 
	 * @return The source connected to this sink *
	 **/
	MediaSource getConnectedSrc();

	/**
	 * 
	 * Asynchronous version of getConnectedSrc: {@link Continuation#onSuccess}
	 * is called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaSink#getConnectedSrc
	 * 
	 **/
	void getConnectedSrc(Continuation<MediaSource> cont);

}
