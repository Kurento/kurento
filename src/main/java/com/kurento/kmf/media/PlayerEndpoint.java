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
 * Retrieves content from seekable sources in reliable mode (does not discard
 * media information) and inject them into <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-kms">KMS</a>. It
 * contains one {@link MediaSource} for each media type detected.
 * 
 **/
@RemoteClass
public interface PlayerEndpoint extends UriEndpoint {

	/**
	 * 
	 * Starts to send data to the endpoint {@link MediaSource}
	 * 
	 **/
	void play();

	/**
	 * 
	 * Asynchronous version of play: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see PlayerEndpoint#play
	 * 
	 **/
	void play(Continuation<Void> cont);

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
	 * Factory for building {@link PlayerEndpoint}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for PlayerEndpoint
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("uri") String uri);
	}

	public interface Builder extends AbstractBuilder<PlayerEndpoint> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for PlayerEndpoint.
		 * 
		 * @param mediaPipeline
		 *            The {@link MediaPipeline} this PlayerEndpoint belongs to.
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for uri in Builder for PlayerEndpoint.
		 * 
		 * @param uri
		 *            URI that will be played
		 * 
		 **/
		public Builder withUri(String uri);

		/**
		 * 
		 * use encoded instead of raw media. If the parameter is false then the
		 * element uses raw media. Changing this parameter can affect stability
		 * severely, as lost key frames lost will not be regenerated. Changing
		 * the media type does not affect to the result except in the
		 * performance (just in the case where original media and target media
		 * are the same) and in the problem with the key frames. We strongly
		 * recommended not to use this parameter because correct behaviour is
		 * not guarantied.
		 * 
		 **/
		public Builder useEncodedMedia();
	}
}
