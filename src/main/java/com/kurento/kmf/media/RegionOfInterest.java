package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.server.Param;

public class RegionOfInterest {

	private List<Point> points;
	private String id;

	public RegionOfInterest(@Param("points") List<Point> points,
			@Param("id") String id) {
		this.points = points;
		this.id = id;
	}

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
