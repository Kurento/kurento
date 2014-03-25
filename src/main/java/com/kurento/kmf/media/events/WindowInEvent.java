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
 * Event generated when an object enters a window.
 * 
 **/
public class WindowInEvent extends MediaEvent {

	/**
	 * 
	 * Opaque String indicating the id of the window entered
	 * 
	 **/
	private String windowId;

	/**
	 * 
	 * Event generated when an object enters a window.
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * @param windowId
	 *            Opaque String indicating the id of the window entered
	 * 
	 **/
	public WindowInEvent(@Param("source") MediaObject source,
			@Param("type") String type, @Param("windowId") String windowId) {
		super(source, type);
		this.windowId = windowId;
	}

	/**
	 * 
	 * Getter for the windowId property
	 * 
	 * @return Opaque String indicating the id of the window entered *
	 **/
	public String getWindowId() {
		return windowId;
	}

	/**
	 * 
	 * Setter for the windowId property
	 * 
	 * @param windowId
	 *            Opaque String indicating the id of the window entered
	 * 
	 **/
	public void setWindowId(String windowId) {
		this.windowId = windowId;
	}

}
