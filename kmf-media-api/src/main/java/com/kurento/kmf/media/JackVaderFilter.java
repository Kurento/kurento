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
 * Filter that detects faces in a video feed. Those on the right half of the
 * feed are overlaid with a pirate hat, and those on the left half are covered
 * by a Darth Vader helmet. This is an example filter, intended to demonstrate
 * how to integrate computer vision capabilities into the multimedia
 * infrastructure.
 * 
 **/
@RemoteClass
public interface JackVaderFilter extends Filter {

	/**
	 * 
	 * Factory for building {@link JackVaderFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for JackVaderFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<JackVaderFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for JackVaderFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
