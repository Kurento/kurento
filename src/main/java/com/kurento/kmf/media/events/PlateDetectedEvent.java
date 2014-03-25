/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * Event raised by a {@link PlateDetectorFilter} when a plate is found in the
 * data streamed.
 * 
 **/
public class PlateDetectedEvent extends MediaEvent {

	/**
	 * 
	 * Plate identification that was detected by the filter
	 * 
	 **/
	private String plate;

	/**
	 * 
	 * Event raised by a {@link PlateDetectorFilter} when a plate is found in
	 * the data streamed.
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * @param plate
	 *            Plate identification that was detected by the filter
	 * 
	 **/
	public PlateDetectedEvent(@Param("source") MediaObject source,
			@Param("type") String type, @Param("plate") String plate) {
		super(source, type);
		this.plate = plate;
	}

	/**
	 * 
	 * Getter for the plate property
	 * 
	 * @return Plate identification that was detected by the filter *
	 **/
	public String getPlate() {
		return plate;
	}

	/**
	 * 
	 * Setter for the plate property
	 * 
	 * @param plate
	 *            Plate identification that was detected by the filter
	 * 
	 **/
	public void setPlate(String plate) {
		this.plate = plate;
	}

}
