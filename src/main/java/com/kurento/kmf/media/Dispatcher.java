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
public interface Dispatcher extends Hub {

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
	 * @param sink
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	void connect(@Param("source") HubPort source, @Param("sink") HubPort sink);

	/**
	 * 
	 * Asynchronous version of connect: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see Dispatcher#connect
	 * 
	 * @param source
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * @param sink
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	void connect(@Param("source") HubPort source, @Param("sink") HubPort sink,
			Continuation<Void> cont);

	/**
	 * 
	 * Factory for building {@link Dispatcher}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for Dispatcher
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<Dispatcher> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for Dispatcher.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline to which the dispatcher belongs}
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
