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
public interface Composite extends Hub {

	/**
	 * 
	 * Factory for building {@link Composite}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for Composite
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<Composite> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for Composite.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline to which the dispatcher belongs}
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
