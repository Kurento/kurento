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
public class CrowdDetectorDirectionEvent extends MediaEvent {

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private float directionAngle;
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
	 * @param directionAngle
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
	 * @return <hr/>
	 *         <b>TODO</b>
	 * 
	 *         FIXME: documentation needed *
	 **/
	public float getDirectionAngle() {
		return directionAngle;
	}

	/**
	 * 
	 * Setter for the directionAngle property
	 * 
	 * @param directionAngle
	 *            <hr/>
	 *            <b>TODO</b>
	 * 
	 *            FIXME: documentation needed
	 * 
	 **/
	public void setDirectionAngle(float directionAngle) {
		this.directionAngle = directionAngle;
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
