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
 * Event raise when a movement direction is detected in a ROI
 * 
 **/
public class CrowdDetectorDirectionEvent extends MediaEvent {

	/**
	 * 
	 * Direction angle of the detected movement in the ROI
	 * 
	 **/
	private float directionAngle;
	/**
	 * 
	 * Opaque String indicating the id of the involved ROI
	 * 
	 **/
	private String roiID;

	/**
	 * 
	 * Event raise when a movement direction is detected in a ROI
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * @param directionAngle
	 *            Direction angle of the detected movement in the ROI
	 * @param roiID
	 *            Opaque String indicating the id of the involved ROI
	 * 
	 **/
	public CrowdDetectorDirectionEvent(@Param("source") MediaObject source,
			@Param("type") String type,
			@Param("directionAngle") float directionAngle,
			@Param("roiID") String roiID) {
		super(source, type);
		this.directionAngle = directionAngle;
		this.roiID = roiID;
	}

	/**
	 * 
	 * Getter for the directionAngle property
	 * 
	 * @return Direction angle of the detected movement in the ROI *
	 **/
	public float getDirectionAngle() {
		return directionAngle;
	}

	/**
	 * 
	 * Setter for the directionAngle property
	 * 
	 * @param directionAngle
	 *            Direction angle of the detected movement in the ROI
	 * 
	 **/
	public void setDirectionAngle(float directionAngle) {
		this.directionAngle = directionAngle;
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
