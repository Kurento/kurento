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
		 *            <hr/>
		 *            <b>TODO</b>
		 * 
		 *            FIXME: documentation needed
		 * 
		 **/
		public Builder withHub(Hub hub);
	}
}
