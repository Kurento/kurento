/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.server.Param;

/**
 * 
 * Region of interest for some events in a video processing filter
 * 
 **/
public class RegionOfInterest {

	/**
	 * 
	 * list of points delimiting the region of interest
	 * 
	 **/
	private List<Point> points;
	/**
	 * 
	 * <hr/>
	 * <b>TODO</b> FIXME: documentation needed
	 * 
	 **/
	private RegionOfInterestConfig regionOfInterestConfig;
	/**
	 * 
	 * identifier of the region of interest
	 * 
	 **/
	private String id;

	/**
	 * 
	 * Create a RegionOfInterest
	 * 
	 **/
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
