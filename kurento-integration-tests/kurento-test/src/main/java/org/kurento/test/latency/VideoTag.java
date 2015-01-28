/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.latency;

/**
 * Video tag for color detection (used in latency control).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class VideoTag {

	private String color;
	private String time;
	private String name;
	private VideoTagType videoTagType;

	public VideoTag(VideoTagType videoTagType, String mapKey) {
		this.videoTagType = videoTagType;
		this.color = "return colorMap['" + mapKey + "'].rgba;";
		this.time = "return colorMap['" + mapKey + "'].time;";
		this.name = mapKey;
	}

	public VideoTag(VideoTagType videoTagType) {
		this.videoTagType = videoTagType;
		this.color = videoTagType.getColor();
		this.time = videoTagType.getTime();
		this.name = videoTagType.getName();
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public VideoTagType getVideoTagType() {
		return videoTagType;
	}

	public void setVideoTagType(VideoTagType videoTagType) {
		this.videoTagType = videoTagType;
	}

}
