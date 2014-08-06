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
 * Endpoint that provides bidirectional content delivery capabilities with
 * remote networked peers through RTP protocol. An {@link RtpEndpoint} contains
 * paired sink and source {@link MediaPad} for audio and video.
 * 
 **/
@RemoteClass
public interface RtpEndpoint extends SdpEndpoint {

	/**
	 * 
	 * Factory for building {@link RtpEndpoint}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for RtpEndpoint
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<RtpEndpoint> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for RtpEndpoint.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the endpoint belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
