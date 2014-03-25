package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.tool.rom.server.Param;

public class CrowdDetectorDirectionEvent extends MediaEvent {

	private float directionAngle;
	private String roiID;

	public CrowdDetectorDirectionEvent(@Param("source") MediaObject source,
			@Param("type") String type,
			@Param("directionAngle") float directionAngle,
			@Param("roiID") String roiID) {
		super(source, type);
		this.directionAngle = directionAngle;
		this.roiID = roiID;
	}

	public float getDirectionAngle() {
		return directionAngle;
	}

	public void setDirectionAngle(float directionAngle) {
		this.directionAngle = directionAngle;
	}

	public String getRoiID() {
		return roiID;
	}

	public void setRoiID(String roiID) {
		this.roiID = roiID;
	}

}
