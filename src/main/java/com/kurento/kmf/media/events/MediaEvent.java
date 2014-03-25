/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * Base for all events raised by elements in the Kurento media server.
 * 
 **/
public class MediaEvent implements Event {

	/**
	 * 
	 * Object that raised the event
	 * 
	 **/
	private MediaObject source;
	/**
	 * 
	 * Type of event that was raised
	 * 
	 **/
	private String type;

	/**
	 * 
	 * Base for all events raised by elements in the Kurento media server.
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * 
	 **/
	public MediaEvent(@Param("source") MediaObject source,
			@Param("type") String type) {
		super();
		this.source = source;
		this.type = type;
	}

	/**
	 * 
	 * Getter for the source property
	 * 
	 * @return Object that raised the event *
	 **/
	public MediaObject getSource() {
		return source;
	}

	/**
	 * 
	 * Setter for the source property
	 * 
	 * @param source
	 *            Object that raised the event
	 * 
	 **/
	public void setSource(MediaObject source) {
		this.source = source;
	}

	/**
	 * 
	 * Getter for the type property
	 * 
	 * @return Type of event that was raised *
	 **/
	public String getType() {
		return type;
	}

	/**
	 * 
	 * Setter for the type property
	 * 
	 * @param type
	 *            Type of event that was raised
	 * 
	 **/
	public void setType(String type) {
		this.type = type;
	}

}
