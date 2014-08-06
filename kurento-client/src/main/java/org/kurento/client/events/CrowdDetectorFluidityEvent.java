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
 * Event raise when a level of fluidity is detected in a ROI
 * 
 **/
public class CrowdDetectorFluidityEvent extends MediaEvent {

	/**
	 * 
	 * Percentage of fluidity in the ROI
	 * 
	 **/
	private float fluidityPercentage;
	/**
	 * 
	 * Level of fluidity in the ROI
	 * 
	 **/
	private int fluidityLevel;
	/**
	 * 
	 * Opaque String indicating the id of the involved ROI
	 * 
	 **/
	private String roiID;

	/**
	 * 
	 * Event raise when a level of fluidity is detected in a ROI
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * @param fluidityPercentage
	 *            Percentage of fluidity in the ROI
	 * @param fluidityLevel
	 *            Level of fluidity in the ROI
	 * @param roiID
	 *            Opaque String indicating the id of the involved ROI
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
	 * @return Percentage of fluidity in the ROI *
	 **/
	public float getFluidityPercentage() {
		return fluidityPercentage;
	}

	/**
	 * 
	 * Setter for the fluidityPercentage property
	 * 
	 * @param fluidityPercentage
	 *            Percentage of fluidity in the ROI
	 * 
	 **/
	public void setFluidityPercentage(float fluidityPercentage) {
		this.fluidityPercentage = fluidityPercentage;
	}

	/**
	 * 
	 * Getter for the fluidityLevel property
	 * 
	 * @return Level of fluidity in the ROI *
	 **/
	public int getFluidityLevel() {
		return fluidityLevel;
	}

	/**
	 * 
	 * Setter for the fluidityLevel property
	 * 
	 * @param fluidityLevel
	 *            Level of fluidity in the ROI
	 * 
	 **/
	public void setFluidityLevel(int fluidityLevel) {
		this.fluidityLevel = fluidityLevel;
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
