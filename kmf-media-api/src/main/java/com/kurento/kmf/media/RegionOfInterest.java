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
	 * data structure for configuration of CrowdDetector regions of interest
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

	/**
	 * 
	 * get list of points delimiting the region of interest
	 * 
	 **/
	public List<Point> getPoints() {
		return points;
	}

	/**
	 * 
	 * set list of points delimiting the region of interest
	 * 
	 **/
	public void setPoints(List<Point> points) {
		this.points = points;
	}

	/**
	 * 
	 * get data structure for configuration of CrowdDetector regions of interest
	 * 
	 **/
	public RegionOfInterestConfig getRegionOfInterestConfig() {
		return regionOfInterestConfig;
	}

	/**
	 * 
	 * set data structure for configuration of CrowdDetector regions of interest
	 * 
	 **/
	public void setRegionOfInterestConfig(
			RegionOfInterestConfig regionOfInterestConfig) {
		this.regionOfInterestConfig = regionOfInterestConfig;
	}

	/**
	 * 
	 * get identifier of the region of interest
	 * 
	 **/
	public String getId() {
		return id;
	}

	/**
	 * 
	 * set identifier of the region of interest
	 * 
	 **/
	public void setId(String id) {
		this.id = id;
	}

}
