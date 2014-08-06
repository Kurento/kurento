/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client.events;

import org.kurento.client.MediaObject;
import org.kurento.client.internal.server.Param;

/**
 * 
 * Event raise when a level of occupancy is detected in a ROI
 * 
 **/
public class CrowdDetectorOccupancyEvent extends MediaEvent {

	/**
	 * 
	 * Percentage of occupancy in the ROI
	 * 
	 **/
	private float occupancyPercentage;
	/**
	 * 
	 * Level of occupancy in the ROI
	 * 
	 **/
	private int occupancyLevel;
	/**
	 * 
	 * Opaque String indicating the id of the involved ROI
	 * 
	 **/
	private String roiID;

	/**
	 * 
	 * Event raise when a level of occupancy is detected in a ROI
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * @param occupancyPercentage
	 *            Percentage of occupancy in the ROI
	 * @param occupancyLevel
	 *            Level of occupancy in the ROI
	 * @param roiID
	 *            Opaque String indicating the id of the involved ROI
	 * 
	 **/
	public CrowdDetectorOccupancyEvent(@Param("source") MediaObject source,
			@Param("type") String type,
			@Param("occupancyPercentage") float occupancyPercentage,
			@Param("occupancyLevel") int occupancyLevel,
			@Param("roiID") String roiID) {
		super(source, type);
		this.occupancyPercentage = occupancyPercentage;
		this.occupancyLevel = occupancyLevel;
		this.roiID = roiID;
	}

	/**
	 * 
	 * Getter for the occupancyPercentage property
	 * 
	 * @return Percentage of occupancy in the ROI *
	 **/
	public float getOccupancyPercentage() {
		return occupancyPercentage;
	}

	/**
	 * 
	 * Setter for the occupancyPercentage property
	 * 
	 * @param occupancyPercentage
	 *            Percentage of occupancy in the ROI
	 * 
	 **/
	public void setOccupancyPercentage(float occupancyPercentage) {
		this.occupancyPercentage = occupancyPercentage;
	}

	/**
	 * 
	 * Getter for the occupancyLevel property
	 * 
	 * @return Level of occupancy in the ROI *
	 **/
	public int getOccupancyLevel() {
		return occupancyLevel;
	}

	/**
	 * 
	 * Setter for the occupancyLevel property
	 * 
	 * @param occupancyLevel
	 *            Level of occupancy in the ROI
	 * 
	 **/
	public void setOccupancyLevel(int occupancyLevel) {
		this.occupancyLevel = occupancyLevel;
	}

	/**
	 * 
	 * Getter for the roiID property
	 * 
	 * @return Opaque String indicating the id of the involved ROI *
	 **/
	public String getRoiID() {
		return roiID;
	}

	/**
	 * 
	 * Setter for the roiID property
	 * 
	 * @param roiID
	 *            Opaque String indicating the id of the involved ROI
	 * 
	 **/
	public void setRoiID(String roiID) {
		this.roiID = roiID;
	}

}
