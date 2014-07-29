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
package com.kurento.demo.internal;

import java.util.HashMap;
import java.util.Map;

import com.kurento.demo.player.PlayerRedirect;
import com.kurento.demo.player.PlayerTunnel;
import com.kurento.demo.playerjson.PlayerJsonRedirect;
import com.kurento.demo.playerjson.PlayerJsonTunnel;

/**
 * Static class which contains a maps of the different URLs to be played in the
 * HTTP Player Handler of this project. For example, in {@link PlayerRedirect},
 * {@link PlayerTunnel}, {@link PlayerJsonRedirect} and {@link PlayerJsonTunnel}
 * handlers, the <code>contentId</code> of the request to this player handlers
 * will be the key in the map of this static class.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
public class VideoURLs {

	public static final String[] small = { "small-3gp", "small-mkv",
			"small-mov", "small-mp4", "small-webm" };

	/**
	 * @since 4.1.1
	 */
	public static final Map<String, String> map;
	static {
		map = new HashMap<String, String>();
		map.put(small[0], "http://files.kurento.org/video/small.3gp");
		map.put(small[1], "http://files.kurento.org/video/small.mkv");
		map.put(small[2], "http://files.kurento.org/video/small.mov");
		map.put(small[3], "http://files.kurento.org/video/small.mp4");
		map.put(small[4], "http://files.kurento.org/video/small.webm");

		map.put("webm", "http://files.kurento.org/video/sintel.webm");
		map.put("mov", "http://files.kurento.org/video/rabbit.mov");
		map.put("mkv", "http://files.kurento.org/video/fiware.mkv");
		map.put("3gp", "http://files.kurento.org/video/blackberry.3gp");
		map.put("ogv", "http://files.kurento.org/video/pacman.ogv");
		map.put("mp4", "http://files.kurento.org/video/chrome.mp4");
		map.put("avi", "http://files.kurento.org/video/car.avi");

		// RTSP streams from YouTube can be obtained using Google Data API at
		// gdata.youtube.com. For instance, for the following video:
		// https://www.youtube.com/watch?v=0CGCy7gC9Zk
		// ... we simply visit this page:
		// http://gdata.youtube.com/feeds/api/videos/0CGCy7gC9Zk
		// ... and obtain the RTSP URL from the returned XML
		map.put("rtsp",
				"rtsp://r1---sn-cg07luee.c.youtube.com/CiILENy73wIaGQmZ9QK4y4Ih0BMYDSANFEgGUgZ2aWRlb3MM/0/0/0/video.3gp");

		map.put("face", "http://files.kurento.org/video/fiwarecut.webm");
		map.put("zbar", "http://files.kurento.org/video/barcodes.webm");
		map.put("crowd", "http://files.kurento.org/video/crowd.mp4");
		map.put("plate", "http://files.kurento.org/video/plates.webm");
		map.put("pointer", "http://files.kurento.org/video/pointerDetector.mp4");
	};

}
