package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.tool.rom.server.Param;

public class CrowdDetectorFluidityEvent extends MediaEvent {

	private float fluidityPercentage;
	private int fluidityLevel;
	private String roiID;

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

	public float getFluidityPercentage() {
		return fluidityPercentage;
	}

	public void setFluidityPercentage(float fluidityPercentage) {
		this.fluidityPercentage = fluidityPercentage;
	}

	public int getFluidityLevel() {
		return fluidityLevel;
	}

	public void setFluidityLevel(int fluidityLevel) {
		this.fluidityLevel = fluidityLevel;
	}

	public String getRoiID() {
		return roiID;
	}

	public void setRoiID(String roiID) {
		this.roiID = roiID;
	}

}
