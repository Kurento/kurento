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
 * <hr/>
 * <b>TODO</b> FIXME: documentation needed
 * 
 **/
public class CrowdDetectorOccupancyEvent extends MediaEvent {

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private float occupancyPercentage;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int occupancyLevel;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private String roiID;

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event raised
	 * 
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            ROM name?
	 * @param occupancyPercentage
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * @param occupancyLevel
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * @param roiID
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
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
	 * @return <hr/>
	 *         <b>TODO</b>
	 * 
	 *         FIXME: documentation needed *
	 **/
	public float getOccupancyPercentage() {
		return occupancyPercentage;
	}

	/**
	 * 
	 * Setter for the occupancyPercentage property
	 * 
	 * @param occupancyPercentage
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	public void setOccupancyPercentage(float occupancyPercentage) {
		this.occupancyPercentage = occupancyPercentage;
	}

	/**
	 * 
	 * Getter for the occupancyLevel property
	 * 
	 * @return <hr/>
	 *         <b>TODO</b>
	 * 
	 *         FIXME: documentation needed *
	 **/
	public int getOccupancyLevel() {
		return occupancyLevel;
	}

	/**
	 * 
	 * Setter for the occupancyLevel property
	 * 
	 * @param occupancyLevel
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	public void setOccupancyLevel(int occupancyLevel) {
		this.occupancyLevel = occupancyLevel;
	}

	/**
	 * 
	 * Getter for the roiID property
	 * 
	 * @return <hr/>
	 *         <b>TODO</b>
	 * 
	 *         FIXME: documentation needed *
	 **/
	public String getRoiID() {
		return roiID;
	}

	/**
	 * 
	 * Setter for the roiID property
	 * 
	 * @param roiID
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	public void setRoiID(String roiID) {
		this.roiID = roiID;
	}

}
