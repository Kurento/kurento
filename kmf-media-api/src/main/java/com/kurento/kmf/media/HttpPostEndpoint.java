/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * An {@link HttpPostEndpoint} contains SINK pads for AUDIO and VIDEO, which
 * provide access to an HTTP file upload function This type of endpoint provide
 * unidirectional communications. Its {@link MediaSource MediaSources } are
 * accessed through the <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-http">HTTP</a>
 * POST method.
 * 
 **/
@RemoteClass
public interface HttpPostEndpoint extends HttpEndpoint {

	/**
	 * Add a {@link MediaEventListener} for event {@link EndOfStreamEvent}.
	 * Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on EndOfStreamEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addEndOfStreamListener(
			MediaEventListener<EndOfStreamEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event {@link EndOfStreamEvent}.
	 * Asynchronous call. Calls Continuation&lt;ListenerRegistration&gt; when it
	 * has been added.
	 * 
	 * @param listener
	 *            Listener to be called on EndOfStreamEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addEndOfStreamListener(MediaEventListener<EndOfStreamEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Factory for building {@link HttpPostEndpoint}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for HttpPostEndpoint
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<HttpPostEndpoint> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for HttpPostEndpoint.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the endpoint belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for disconnectionTimeout in Builder for
		 * HttpPostEndpoint.
		 * 
		 * @param disconnectionTimeout
		 *            This is the time that an http endpoint will wait for a
		 *            reconnection, in case an HTTP connection is lost.
		 * 
		 **/
		public Builder withDisconnectionTimeout(int disconnectionTimeout);

		/**
		 * 
		 * configures the endpoint to use encoded media instead of raw media. If
		 * the parameter is not set then the element uses raw media. Changing
		 * this parameter could affect in a severe way to stability because key
		 * frames lost will not be generated. Changing the media type does not
		 * affect to the result except in the performance (just in the case
		 * where original media and target media are the same) and in the
		 * problem with the key frames. We strongly recommended not to use this
		 * parameter because correct behaviour is not guarantied.
		 * 
		 **/
		public Builder useEncodedMedia();
	}
}
