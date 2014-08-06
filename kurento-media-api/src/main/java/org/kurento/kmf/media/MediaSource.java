/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media;

import java.util.List;

import org.kurento.tool.rom.RemoteClass;
import org.kurento.tool.rom.server.Param;

/**
 * 
 * Special type of pad, used by a media element to generate a media stream.
 * 
 **/
@RemoteClass
public interface MediaSource extends MediaPad {

	/**
	 * 
	 * Gets all the {@link MediaSink MediaSinks} to which this source is
	 * connected
	 * 
	 * @return the list of sinks that the source is connected to *
	 **/
	List<MediaSink> getConnectedSinks();

	/**
	 * 
	 * Asynchronous version of getConnectedSinks: {@link Continuation#onSuccess}
	 * is called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaSource#getConnectedSinks
	 * 
	 **/
	void getConnectedSinks(Continuation<List<MediaSink>> cont);

	/**
	 * 
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * 
	 **/
	void connect(@Param("sink") MediaSink sink);

	/**
	 * 
	 * Asynchronous version of connect: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see MediaSource#connect
	 * 
	 * @param sink
	 *            The sink to connect this source
	 * 
	 **/
	void connect(@Param("sink") MediaSink sink, Continuation<Void> cont);

}
