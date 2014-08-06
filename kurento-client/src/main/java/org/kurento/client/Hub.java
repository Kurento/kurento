/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.FactoryMethod;

/**
 * 
 * A Hub is a routing {@link MediaObject}. It connects several {@link Endpoint
 * endpoints } together
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
