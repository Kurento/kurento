/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.FactoryMethod;

/**
 * 
 * <hr/>
 * <b>TODO</b> FIXME: documentation needed
 * 
 **/
@RemoteClass
public interface Hub extends MediaObject {

	/**
	 * Get a {@link HubPort}.{@link Builder} for this Hub
	 * 
	 **/
	@FactoryMethod("hub")
	public abstract HubPort.Builder newHubPort();

}
