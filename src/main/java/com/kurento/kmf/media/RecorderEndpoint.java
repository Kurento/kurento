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
 * Provides function to store contents in reliable mode (doesn't discard data).
 * It contains {@link MediaSink} pads for audio and video.
 * 
 **/
@RemoteClass
public interface RecorderEndpoint extends UriEndpoint {

	/**
	 * 
	 * Starts storing media received through the {@link MediaSink} pad
	 * 
	 **/
	void record();

	/**
	 * 
	 * Asynchronous version of record: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see RecorderEndpoint#record
	 * 
	 **/
	void record(Continuation<Void> cont);

	/**
	 * 
	 * Factory for building {@link RecorderEndpoint}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for RecorderEndpoint
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("uri") String uri);
	}

	public interface Builder extends AbstractBuilder<RecorderEndpoint> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for RecorderEndpoint.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the endpoint belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for uri in Builder for RecorderEndpoint.
		 * 
		 * @param uri
		 *            URI where the recording will be stored
		 * 
		 **/
		public Builder withUri(String uri);

		/**
		 * 
		 * Sets a value for mediaProfile in Builder for RecorderEndpoint.
		 * 
		 * @param mediaProfile
		 *            Choose either a {@link #MediaProfileSpecType.WEBM} or a
		 *            {@link #MediaProfileSpecType.MP4} profile for recording
		 * 
		 **/
		public Builder withMediaProfile(MediaProfileSpecType mediaProfile);

		/**
		 * 
		 * Forces the recorder endpoint to finish processing data when an <a
		 * href
		 * ="http://www.kurento.org/docs/current/glossary.html#term-eos">EOS</a>
		 * is detected in the stream
		 * 
		 **/
		public Builder stopOnEndOfStream();
	}
}
