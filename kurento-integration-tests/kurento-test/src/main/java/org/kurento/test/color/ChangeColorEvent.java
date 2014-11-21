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
package org.kurento.test.color;

/**
 * Events for color change detection.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ChangeColorEvent {

	private VideoTag videoTag;
	private long time;
	private String color;

	public ChangeColorEvent(VideoTag videoTag, long time, String color) {
		this.videoTag = videoTag;
		this.time = time;
		this.color = color;
	}

	public VideoTag getVideoTag() {
		return videoTag;
	}

	public long getTime() {
		return time;
	}

	public void setVideoTag(VideoTag videoTag) {
		this.videoTag = videoTag;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

}
