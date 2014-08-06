/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media;

import org.kurento.tool.rom.RemoteClass;
import org.kurento.tool.rom.server.Param;

/**
 * 
 * A {@link Hub} that sends a given source to all the connected sinks
 * 
 **/
@RemoteClass
public interface DispatcherOneToMany extends Hub {

	/**
	 * 
	 * Sets the source port that will be connected to the sinks of every
	 * {@link HubPort} of the dispatcher
	 * 
	 * @param source
	 *            source to be broadcasted
	 * 
	 **/
	void setSource(@Param("source") HubPort source);

	/**
	 * 
	 * Asynchronous version of setSource: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see DispatcherOneToMany#setSource
	 * 
	 * @param source
	 *            source to be broadcasted
	 * 
	 **/
	void setSource(@Param("source") HubPort source, Continuation<Void> cont);

	/**
	 * 
	 * Remove the source port and stop the media pipeline.
	 * 
	 **/
	void removeSource();

	/**
	 * 
	 * Asynchronous version of removeSource: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see DispatcherOneToMany#removeSource
	 * 
	 **/
	void removeSource(Continuation<Void> cont);

	/**
	 * 
	 * Factory for building {@link DispatcherOneToMany}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for DispatcherOneToMany
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<DispatcherOneToMany> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for DispatcherOneToMany.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the dispatcher belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
