/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;


/**
 * 
 * data structure for configuration of CrowdDetector regions of interest
 * 
 **/
public class RegionOfInterestConfig {

	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int occupancyLevelMin;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int occupancyLevelMed;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int occupancyLevelMax;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int occupancyNumFramesToEvent;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int fluidityLevelMin;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int fluidityLevelMed;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int fluidityLevelMax;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int fluidityNumFramesToEvent;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private boolean sendOpticalFlowEvent;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int opticalFlowNumFramesToEvent;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int opticalFlowNumFramesToReset;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private int opticalFlowAngleOffset;

	/**
	 * 
	 * Create a RegionOfInterestConfig
	 * 
	 **/
	public RegionOfInterestConfig() {
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOccupancyLevelMin() {
		return occupancyLevelMin;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOccupancyLevelMin(int occupancyLevelMin) {
		this.occupancyLevelMin = occupancyLevelMin;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOccupancyLevelMed() {
		return occupancyLevelMed;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOccupancyLevelMed(int occupancyLevelMed) {
		this.occupancyLevelMed = occupancyLevelMed;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOccupancyLevelMax() {
		return occupancyLevelMax;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOccupancyLevelMax(int occupancyLevelMax) {
		this.occupancyLevelMax = occupancyLevelMax;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOccupancyNumFramesToEvent() {
		return occupancyNumFramesToEvent;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOccupancyNumFramesToEvent(int occupancyNumFramesToEvent) {
		this.occupancyNumFramesToEvent = occupancyNumFramesToEvent;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getFluidityLevelMin() {
		return fluidityLevelMin;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setFluidityLevelMin(int fluidityLevelMin) {
		this.fluidityLevelMin = fluidityLevelMin;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getFluidityLevelMed() {
		return fluidityLevelMed;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setFluidityLevelMed(int fluidityLevelMed) {
		this.fluidityLevelMed = fluidityLevelMed;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getFluidityLevelMax() {
		return fluidityLevelMax;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setFluidityLevelMax(int fluidityLevelMax) {
		this.fluidityLevelMax = fluidityLevelMax;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getFluidityNumFramesToEvent() {
		return fluidityNumFramesToEvent;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setFluidityNumFramesToEvent(int fluidityNumFramesToEvent) {
		this.fluidityNumFramesToEvent = fluidityNumFramesToEvent;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public boolean getSendOpticalFlowEvent() {
		return sendOpticalFlowEvent;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setSendOpticalFlowEvent(boolean sendOpticalFlowEvent) {
		this.sendOpticalFlowEvent = sendOpticalFlowEvent;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOpticalFlowNumFramesToEvent() {
		return opticalFlowNumFramesToEvent;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOpticalFlowNumFramesToEvent(int opticalFlowNumFramesToEvent) {
		this.opticalFlowNumFramesToEvent = opticalFlowNumFramesToEvent;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOpticalFlowNumFramesToReset() {
		return opticalFlowNumFramesToReset;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOpticalFlowNumFramesToReset(int opticalFlowNumFramesToReset) {
		this.opticalFlowNumFramesToReset = opticalFlowNumFramesToReset;
	}

	/**
	 * 
	 * get
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public int getOpticalFlowAngleOffset() {
		return opticalFlowAngleOffset;
	}

	/**
	 * 
	 * set
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	public void setOpticalFlowAngleOffset(int opticalFlowAngleOffset) {
		this.opticalFlowAngleOffset = opticalFlowAngleOffset;
	}

}
