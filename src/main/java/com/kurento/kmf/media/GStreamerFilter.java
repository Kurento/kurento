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
 * This is a generic filter interface, that creates GStreamer filters in the
 * media server.
 * 
 **/
@RemoteClass
public interface GStreamerFilter extends Filter {

	/**
	 * 
	 * Factory for building {@link GStreamerFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for GStreamerFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("command") String command);
	}

	public interface Builder extends AbstractBuilder<GStreamerFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for GStreamerFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for command in Builder for GStreamerFilter.
		 * 
		 * @param command
		 *            command that would be used to instantiate the filter, as
		 *            in `gst-launch
		 *            <http://rpm.pbone.net/index.php3/stat/45/idpl
		 *            /19531544/numer/1/nazwa/gst-launch-1.0>`__
		 * 
		 **/
		public Builder withCommand(String command);
	}
}
