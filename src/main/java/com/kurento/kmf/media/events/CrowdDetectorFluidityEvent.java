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
public class CrowdDetectorFluidityEvent extends MediaEvent {

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private float fluidityPercentage;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int fluidityLevel;
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
	 *            Type of event that was raised
	 * @param fluidityPercentage
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * @param fluidityLevel
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
	public CrowdDetectorFluidityEvent(@Param("source") MediaObject source,
			@Param("type") String type,
			@Param("fluidityPercentage") float fluidityPercentage,
			@Param("fluidityLevel") int fluidityLevel,
			@Param("roiID") String roiID) {
		super(source, type);
		this.fluidityPercentage = fluidityPercentage;
		this.fluidityLevel = fluidityLevel;
		this.roiID = roiID;
	}

	/**
	 * 
	 * Getter for the fluidityPercentage property
	 * 
	 * @return <hr/>
	 *         <b>TODO</b>
	 * 
	 *         FIXME: documentation needed *
	 **/
	public float getFluidityPercentage() {
		return fluidityPercentage;
	}

	/**
	 * 
	 * Setter for the fluidityPercentage property
	 * 
	 * @param fluidityPercentage
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	public void setFluidityPercentage(float fluidityPercentage) {
		this.fluidityPercentage = fluidityPercentage;
	}

	/**
	 * 
	 * Getter for the fluidityLevel property
	 * 
	 * @return <hr/>
	 *         <b>TODO</b>
	 * 
	 *         FIXME: documentation needed *
	 **/
	public int getFluidityLevel() {
		return fluidityLevel;
	}

	/**
	 * 
	 * Setter for the fluidityLevel property
	 * 
	 * @param fluidityLevel
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	public void setFluidityLevel(int fluidityLevel) {
		this.fluidityLevel = fluidityLevel;
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
