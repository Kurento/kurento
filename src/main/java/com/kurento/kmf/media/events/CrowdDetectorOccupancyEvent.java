package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.tool.rom.server.Param;

public class CrowdDetectorOccupancyEvent extends MediaEvent {

	private float occupancyPercentage;
	private int occupancyLevel;
	private String roiID;

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

	public float getOccupancyPercentage() {
		return occupancyPercentage;
	}

	public void setOccupancyPercentage(float occupancyPercentage) {
		this.occupancyPercentage = occupancyPercentage;
	}

	public int getOccupancyLevel() {
		return occupancyLevel;
	}

	public void setOccupancyLevel(int occupancyLevel) {
		this.occupancyLevel = occupancyLevel;
	}

	public String getRoiID() {
		return roiID;
	}

	public void setRoiID(String roiID) {
		this.roiID = roiID;
	}

}
