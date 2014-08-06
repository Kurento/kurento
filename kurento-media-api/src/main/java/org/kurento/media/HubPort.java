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
 * This {@link MediaElement} specifies a connection with a {@link Hub}
 * 
 **/
@RemoteClass
public interface HubPort extends MediaElement {

	/**
	 * 
	 * Factory for building {@link HubPort}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for HubPort
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(@Param("hub") Hub hub);
	}

	public interface Builder extends AbstractBuilder<HubPort> {

		/**
		 * 
		 * Sets a value for hub in Builder for HubPort.
		 * 
		 * @param hub
		 *            {@link Hub} to which this port belongs
		 * 
		 **/
		public Builder withHub(Hub hub);
	}
}
