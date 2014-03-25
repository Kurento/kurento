package com.kurento.kmf.media;


public class RegionOfInterestConfig {

	private int occupancyLevelMin;
	private int occupancyLevelMed;
	private int occupancyLevelMax;
	private int occupancyNumFramesToEvent;
	private int fluidityLevelMin;
	private int fluidityLevelMed;
	private int fluidityLevelMax;
	private int fluidityNumFramesToEvent;
	private boolean sendOpticalFlowEvent;
	private int opticalFlowNumFramesToEvent;
	private int opticalFlowNumFramesToReset;
	private int opticalFlowAngleOffset;

	public RegionOfInterestConfig() {
	}

	public int getOccupancyLevelMin() {
		return occupancyLevelMin;
	}

	public void setOccupancyLevelMin(int occupancyLevelMin) {
		this.occupancyLevelMin = occupancyLevelMin;
	}

	public int getOccupancyLevelMed() {
		return occupancyLevelMed;
	}

	public void setOccupancyLevelMed(int occupancyLevelMed) {
		this.occupancyLevelMed = occupancyLevelMed;
	}

	public int getOccupancyLevelMax() {
		return occupancyLevelMax;
	}

	public void setOccupancyLevelMax(int occupancyLevelMax) {
		this.occupancyLevelMax = occupancyLevelMax;
	}

	public int getOccupancyNumFramesToEvent() {
		return occupancyNumFramesToEvent;
	}

	public void setOccupancyNumFramesToEvent(int occupancyNumFramesToEvent) {
		this.occupancyNumFramesToEvent = occupancyNumFramesToEvent;
	}

	public int getFluidityLevelMin() {
		return fluidityLevelMin;
	}

	public void setFluidityLevelMin(int fluidityLevelMin) {
		this.fluidityLevelMin = fluidityLevelMin;
	}

	public int getFluidityLevelMed() {
		return fluidityLevelMed;
	}

	public void setFluidityLevelMed(int fluidityLevelMed) {
		this.fluidityLevelMed = fluidityLevelMed;
	}

	public int getFluidityLevelMax() {
		return fluidityLevelMax;
	}

	public void setFluidityLevelMax(int fluidityLevelMax) {
		this.fluidityLevelMax = fluidityLevelMax;
	}

	public int getFluidityNumFramesToEvent() {
		return fluidityNumFramesToEvent;
	}

	public void setFluidityNumFramesToEvent(int fluidityNumFramesToEvent) {
		this.fluidityNumFramesToEvent = fluidityNumFramesToEvent;
	}

	public boolean getSendOpticalFlowEvent() {
		return sendOpticalFlowEvent;
	}

	public void setSendOpticalFlowEvent(boolean sendOpticalFlowEvent) {
		this.sendOpticalFlowEvent = sendOpticalFlowEvent;
	}

	public int getOpticalFlowNumFramesToEvent() {
		return opticalFlowNumFramesToEvent;
	}

	public void setOpticalFlowNumFramesToEvent(int opticalFlowNumFramesToEvent) {
		this.opticalFlowNumFramesToEvent = opticalFlowNumFramesToEvent;
	}

	public int getOpticalFlowNumFramesToReset() {
		return opticalFlowNumFramesToReset;
	}

	public void setOpticalFlowNumFramesToReset(int opticalFlowNumFramesToReset) {
		this.opticalFlowNumFramesToReset = opticalFlowNumFramesToReset;
	}

	public int getOpticalFlowAngleOffset() {
		return opticalFlowAngleOffset;
	}

	public void setOpticalFlowAngleOffset(int opticalFlowAngleOffset) {
		this.opticalFlowAngleOffset = opticalFlowAngleOffset;
	}

}
