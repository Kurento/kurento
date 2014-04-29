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
package com.kurento.demo.cpbrazil.youtube;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.api.services.youtube.model.Video;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public class YouTubeTest {

	private static final String PLAYLIST_TOKEN = "PL58tWS2XjtialwG-eWDYoFwQpHTd5vDEE";

	@Ignore
	@Test
	public void testInsertToPlaylist() throws IOException {

		String playlistItemId = Playlists.insertItem(PLAYLIST_TOKEN,
				"JooP-i1sJHs");
		Assert.assertNotNull(playlistItemId);
		Playlists.removeItem(playlistItemId);
	}

	@Ignore
	@Test
	public void testUpload() throws IOException {
		List<String> tags = newArrayList("FI-WARE", "Kurento", "FUN-LAB",
				"GSyC", "URJC", "Campus Party", "WebRTC",
				"Software Engineering", "Augmented Reality", "Computer Vision",
				"Super Mario", "Sonic", "Street Fighter", "Donkey Kong");
		Video video = Videos.upload(
				"http://193.147.51.29/campus-party-1389874842998",
				PLAYLIST_TOKEN, tags);
		Assert.assertNotNull(video);
		Videos.delete(video);
	}
}
