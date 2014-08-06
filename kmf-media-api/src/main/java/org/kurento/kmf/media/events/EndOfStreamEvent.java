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
 * Event raised when the stream that the element sends out is finished. An
 * element receiving this event will generally just process any buffered data,
 * and then forward the event further downstream.
 * 
 **/
public class EndOfStreamEvent extends MediaEvent {

	/**
	 * 
	 * Event raised when the stream that the element sends out is finished. An
	 * element receiving this event will generally just process any buffered
	 * data, and then forward the event further downstream.
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * 
	 **/
	public EndOfStreamEvent(@Param("source") MediaObject source,
			@Param("type") String type) {
		super(source, type);
	}

}
