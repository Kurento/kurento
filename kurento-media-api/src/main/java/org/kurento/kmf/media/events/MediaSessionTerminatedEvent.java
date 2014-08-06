/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media.events;

import org.kurento.kmf.media.MediaObject;
import org.kurento.tool.rom.server.Param;

/**
 * 
 * Event raised when a session is terminated. This event has no data.
 * 
 **/
public class MediaSessionTerminatedEvent extends MediaEvent {

	/**
	 * 
	 * Event raised when a session is terminated. This event has no data.
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * 
	 **/
	public MediaSessionTerminatedEvent(@Param("source") MediaObject source,
			@Param("type") String type) {
		super(source, type);
	}

}
