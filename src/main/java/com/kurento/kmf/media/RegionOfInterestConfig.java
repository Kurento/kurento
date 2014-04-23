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
	 * minimun occupancy percentage in the ROI to send occupancy events
	 * 
	 **/
	private int occupancyLevelMin;
	/**
	 * 
	 * send occupancy level = 1 if the occupancy percentage is between
	 * occupancy_level_min and this level
	 * 
	 **/
	private int occupancyLevelMed;
	/**
	 * 
	 * send occupancy level = 2 if the occupancy percentage is between
	 * occupancy_level_med and this level, and send occupancy level = 3 if the
	 * occupancy percentage is between this level and 100
	 * 
	 **/
	private int occupancyLevelMax;
	/**
	 * 
	 * number of consecutive frames that a new occupancy level has to be
	 * detected to recognize it as a occupancy level change. A new occupancy
	 * event will be send
	 * 
	 **/
	private int occupancyNumFramesToEvent;
	/**
	 * 
	 * minimun fluidity percentage in the ROI to send fluidity events
	 * 
	 **/
	private int fluidityLevelMin;
	/**
	 * 
	 * send fluidity level = 1 if the fluidity percentage is between
	 * fluidity_level_min and this level
	 * 
	 **/
	private int fluidityLevelMed;
	/**
	 * 
	 * send fluidity level = 2 if the fluidity percentage is between
	 * fluidity_level_med and this level, and send fluidity level = 3 if the
	 * fluidity percentage is between this level and 100
	 * 
	 **/
	private int fluidityLevelMax;
	/**
	 * 
	 * number of consecutive frames that a new fluidity level has to be detected
	 * to recognize it as a fluidity level change. A new fluidity event will be
	 * send
	 * 
	 **/
	private int fluidityNumFramesToEvent;
	/**
	 * 
	 * Enable/disable the movement direction detection into the ROI
	 * 
	 **/
	private boolean sendOpticalFlowEvent;
	/**
	 * 
	 * number of consecutive frames that a new direction of movement has to be
	 * detected to recognize a new movement direction. A new direction event
	 * will be send
	 * 
	 **/
	private int opticalFlowNumFramesToEvent;
	/**
	 * 
	 * number of consecutive frames in order to reset the counter of repeated
	 * directions
	 * 
	 **/
	private int opticalFlowNumFramesToReset;
	/**
	 * 
	 * Direction of the movement. The angle could have four different values:
	 * left (0), up (90), right (180) and down (270). This cartesian axis could
	 * be rotated adding an angle offset
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
	 * get minimun occupancy percentage in the ROI to send occupancy events
	 * 
	 **/
	public int getOccupancyLevelMin() {
		return occupancyLevelMin;
	}

	/**
	 * 
	 * set minimun occupancy percentage in the ROI to send occupancy events
	 * 
	 **/
	public void setOccupancyLevelMin(int occupancyLevelMin) {
		this.occupancyLevelMin = occupancyLevelMin;
	}

	/**
	 * 
	 * get send occupancy level = 1 if the occupancy percentage is between
	 * occupancy_level_min and this level
	 * 
	 **/
	public int getOccupancyLevelMed() {
		return occupancyLevelMed;
	}

	/**
	 * 
	 * set send occupancy level = 1 if the occupancy percentage is between
	 * occupancy_level_min and this level
	 * 
	 **/
	public void setOccupancyLevelMed(int occupancyLevelMed) {
		this.occupancyLevelMed = occupancyLevelMed;
	}

	/**
	 * 
	 * get send occupancy level = 2 if the occupancy percentage is between
	 * occupancy_level_med and this level, and send occupancy level = 3 if the
	 * occupancy percentage is between this level and 100
	 * 
	 **/
	public int getOccupancyLevelMax() {
		return occupancyLevelMax;
	}

	/**
	 * 
	 * set send occupancy level = 2 if the occupancy percentage is between
	 * occupancy_level_med and this level, and send occupancy level = 3 if the
	 * occupancy percentage is between this level and 100
	 * 
	 **/
	public void setOccupancyLevelMax(int occupancyLevelMax) {
		this.occupancyLevelMax = occupancyLevelMax;
	}

	/**
	 * 
	 * get number of consecutive frames that a new occupancy level has to be
	 * detected to recognize it as a occupancy level change. A new occupancy
	 * event will be send
	 * 
	 **/
	public int getOccupancyNumFramesToEvent() {
		return occupancyNumFramesToEvent;
	}

	/**
	 * 
	 * set number of consecutive frames that a new occupancy level has to be
	 * detected to recognize it as a occupancy level change. A new occupancy
	 * event will be send
	 * 
	 **/
	public void setOccupancyNumFramesToEvent(int occupancyNumFramesToEvent) {
		this.occupancyNumFramesToEvent = occupancyNumFramesToEvent;
	}

	/**
	 * 
	 * get minimun fluidity percentage in the ROI to send fluidity events
	 * 
	 **/
	public int getFluidityLevelMin() {
		return fluidityLevelMin;
	}

	/**
	 * 
	 * set minimun fluidity percentage in the ROI to send fluidity events
	 * 
	 **/
	public void setFluidityLevelMin(int fluidityLevelMin) {
		this.fluidityLevelMin = fluidityLevelMin;
	}

	/**
	 * 
	 * get send fluidity level = 1 if the fluidity percentage is between
	 * fluidity_level_min and this level
	 * 
	 **/
	public int getFluidityLevelMed() {
		return fluidityLevelMed;
	}

	/**
	 * 
	 * set send fluidity level = 1 if the fluidity percentage is between
	 * fluidity_level_min and this level
	 * 
	 **/
	public void setFluidityLevelMed(int fluidityLevelMed) {
		this.fluidityLevelMed = fluidityLevelMed;
	}

	/**
	 * 
	 * get send fluidity level = 2 if the fluidity percentage is between
	 * fluidity_level_med and this level, and send fluidity level = 3 if the
	 * fluidity percentage is between this level and 100
	 * 
	 **/
	public int getFluidityLevelMax() {
		return fluidityLevelMax;
	}

	/**
	 * 
	 * set send fluidity level = 2 if the fluidity percentage is between
	 * fluidity_level_med and this level, and send fluidity level = 3 if the
	 * fluidity percentage is between this level and 100
	 * 
	 **/
	public void setFluidityLevelMax(int fluidityLevelMax) {
		this.fluidityLevelMax = fluidityLevelMax;
	}

	/**
	 * 
	 * get number of consecutive frames that a new fluidity level has to be
	 * detected to recognize it as a fluidity level change. A new fluidity event
	 * will be send
	 * 
	 **/
	public int getFluidityNumFramesToEvent() {
		return fluidityNumFramesToEvent;
	}

	/**
	 * 
	 * set number of consecutive frames that a new fluidity level has to be
	 * detected to recognize it as a fluidity level change. A new fluidity event
	 * will be send
	 * 
	 **/
	public void setFluidityNumFramesToEvent(int fluidityNumFramesToEvent) {
		this.fluidityNumFramesToEvent = fluidityNumFramesToEvent;
	}

	/**
	 * 
	 * get Enable/disable the movement direction detection into the ROI
	 * 
	 **/
	public boolean getSendOpticalFlowEvent() {
		return sendOpticalFlowEvent;
	}

	/**
	 * 
	 * set Enable/disable the movement direction detection into the ROI
	 * 
	 **/
	public void setSendOpticalFlowEvent(boolean sendOpticalFlowEvent) {
		this.sendOpticalFlowEvent = sendOpticalFlowEvent;
	}

	/**
	 * 
	 * get number of consecutive frames that a new direction of movement has to
	 * be detected to recognize a new movement direction. A new direction event
	 * will be send
	 * 
	 **/
	public int getOpticalFlowNumFramesToEvent() {
		return opticalFlowNumFramesToEvent;
	}

	/**
	 * 
	 * set number of consecutive frames that a new direction of movement has to
	 * be detected to recognize a new movement direction. A new direction event
	 * will be send
	 * 
	 **/
	public void setOpticalFlowNumFramesToEvent(int opticalFlowNumFramesToEvent) {
		this.opticalFlowNumFramesToEvent = opticalFlowNumFramesToEvent;
	}

	/**
	 * 
	 * get number of consecutive frames in order to reset the counter of
	 * repeated directions
	 * 
	 **/
	public int getOpticalFlowNumFramesToReset() {
		return opticalFlowNumFramesToReset;
	}

	/**
	 * 
	 * set number of consecutive frames in order to reset the counter of
	 * repeated directions
	 * 
	 **/
	public void setOpticalFlowNumFramesToReset(int opticalFlowNumFramesToReset) {
		this.opticalFlowNumFramesToReset = opticalFlowNumFramesToReset;
	}

	/**
	 * 
	 * get Direction of the movement. The angle could have four different
	 * values: left (0), up (90), right (180) and down (270). This cartesian
	 * axis could be rotated adding an angle offset
	 * 
	 **/
	public int getOpticalFlowAngleOffset() {
		return opticalFlowAngleOffset;
	}

	/**
	 * 
	 * set Direction of the movement. The angle could have four different
	 * values: left (0), up (90), right (180) and down (270). This cartesian
	 * axis could be rotated adding an angle offset
	 * 
	 **/
	public void setOpticalFlowAngleOffset(int opticalFlowAngleOffset) {
		this.opticalFlowAngleOffset = opticalFlowAngleOffset;
	}

}
