/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.demo;

import java.util.HashMap;
import java.util.Map;

/**
 * Static class which contains a maps of the different URLs to be played in the
 * HTTP Player Handler of this project. For example, in
 * {@link PlayerHttpJsonHandler} and {@link PlayerHttpHandler} handlers, the
 * <code>contentId</code> of the request to this player handlers will be the key
 * in the map of this static class.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
public class VideoURLs {

	public static final Map<String, String> map;
	static {
		map = new HashMap<String, String>();
		map.put("webm", "file:///opt/video/sintel.webm");
		map.put("mov", "file:///opt/video/rabbit.mov");
		map.put("mkv", "file:///opt/video/fiware.mkv");
		map.put("3gp", "file:///opt/video/blackberry.3gp");
		map.put("ogv", "file:///opt/video/pacman.ogv");
		map.put("avi", "file:///opt/video/car.avi");
		map.put("mp4", "file:///opt/video/chrome.mp4");
		map.put("jack", "file:///opt/video/fiwarecut.webm");
		map.put("zbar", "file:///opt/video/barcodes.webm");
	};

}
