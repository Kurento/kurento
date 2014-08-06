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
 * WebRtcEndpoint interface. This type of <code>Endpoint</code> offers media
 * streaming using WebRTC.
 * 
 **/
@RemoteClass
public interface WebRtcEndpoint extends SdpEndpoint {

	/**
	 * 
	 * Factory for building {@link WebRtcEndpoint}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for WebRtcEndpoint
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<WebRtcEndpoint> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for WebRtcEndpoint.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the endpoint belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
