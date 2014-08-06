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
 * An <code>HttpGetEndpoint</code> contains SOURCE pads for AUDIO and VIDEO,
 * delivering media using HTML5 pseudo-streaming mechanism. This type of
 * endpoint provide unidirectional communications. Its {@link MediaSink} is
 * associated with the HTTP GET method
 * 
 **/
@RemoteClass
public interface HttpGetEndpoint extends HttpEndpoint {

	/**
	 * 
	 * Factory for building {@link HttpGetEndpoint}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for HttpGetEndpoint
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<HttpGetEndpoint> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for HttpGetEndpoint.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the endpoint belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * raise a :rom:evnt:`MediaSessionTerminated` event when the associated
		 * player raises a :rom:evnt:`EndOfStream`, and thus terminate the media
		 * session
		 * 
		 **/
		public Builder terminateOnEOS();

		/**
		 * 
		 * Sets a value for mediaProfile in Builder for HttpGetEndpoint.
		 * 
		 * @param mediaProfile
		 *            the :rom:enum:`MediaProfileSpecType` (WEBM, MP4...) for
		 *            the endpoint
		 * 
		 **/
		public Builder withMediaProfile(MediaProfileSpecType mediaProfile);

		/**
		 * 
		 * Sets a value for disconnectionTimeout in Builder for HttpGetEndpoint.
		 * 
		 * @param disconnectionTimeout
		 *            disconnection timeout in seconds.
		 * 
		 *            This is the time that an http endpoint will wait for a
		 *            reconnection, in case an HTTP connection is lost.
		 * 
		 **/
		public Builder withDisconnectionTimeout(int disconnectionTimeout);
	}
}
