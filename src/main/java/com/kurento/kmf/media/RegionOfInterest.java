package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.server.Param;

public class RegionOfInterest {

	private List<Point> points;
	private RegionOfInterestConfig regionOfInterestConfig;
	private String id;

	public RegionOfInterest(
			@Param("points") List<Point> points,
			@Param("regionOfInterestConfig") RegionOfInterestConfig regionOfInterestConfig,
			@Param("id") String id) {
		this.points = points;
		this.regionOfInterestConfig = regionOfInterestConfig;
		this.id = id;
	}

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}

	public RegionOfInterestConfig getRegionOfInterestConfig() {
		return regionOfInterestConfig;
	}

	public void setRegionOfInterestConfig(
			RegionOfInterestConfig regionOfInterestConfig) {
		this.regionOfInterestConfig = regionOfInterestConfig;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
