/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * <hr/>
 * <b>TODO</b> FIXME: documentation needed
 * 
 **/
@RemoteClass
public interface DispatcherOneToMany extends Hub {

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 * @param source
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
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
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	void setSource(@Param("source") HubPort source, Continuation<Void> cont);

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
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
		 *            the {@link MediaPipeline to which the dispatcher belongs}
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
