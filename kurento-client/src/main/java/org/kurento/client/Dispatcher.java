/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.Param;

/**
 * 
 * A {@link Hub} that allows routing between arbitrary port pairs
 * 
 **/
@RemoteClass
public interface Dispatcher extends Hub {

	/**
	 * 
	 * Connects each corresponding :rom:enum:`MediaType` of the given source
	 * port with the sink port.
	 * 
	 * @param source
	 *            Source port to be connected
	 * @param sink
	 *            Sink port to be connected
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
	 *            Source port to be connected
	 * @param sink
	 *            Sink port to be connected
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
		 *            the {@link MediaPipeline} to which the dispatcher belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
