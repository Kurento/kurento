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
package com.kurento.demo.cpbrazil.youtube;

import static com.kurento.demo.cpbrazil.youtube.Auth.HTTP_TRANSPORT;
import static com.kurento.demo.cpbrazil.youtube.Auth.JSON_FACTORY;
import static com.kurento.demo.cpbrazil.youtube.Auth.authorise;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.common.collect.Lists;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * Logic for handling YouTube playlists.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.1
 * 
 */
public class Playlists {

	private static final Logger log = LoggerFactory.getLogger(Playlists.class);

	/** Global instance of Youtube object to make all API requests. */
	private static YouTube youtube;

	static {
		// Scope required to update a video YouTube.
		List<String> scopes = Lists.newArrayList(
				"https://www.googleapis.com/auth/youtube",
				"https://www.googleapis.com/auth/youtube.upload");

		try {
			File credentialStoreFile = Auth.inputStreamToFile(Videos.class
					.getResourceAsStream("/youtube-api-uploadvideo.json"));

			Credential credential = authorise(scopes, credentialStoreFile);
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					credential).setApplicationName("kurento-playlist").build();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}
	}

	/**
	 * Create a playlist item with the specified video ID and add it to the
	 * specified playlist.
	 * 
	 * @param playlistId
	 *            assign to newly created playlistitem
	 * @param videoId
	 *            YouTube video id to add to playlistitem
	 * @return The ID that YouTube uses to uniquely identify the playlist item.
	 *         The value returned may be null.
	 * @throws IOException
	 */
	public static String insertItem(String playlistId, String videoId)
			throws IOException {

		// Define a resourceId that identifies the video being added to the
		// playlist.
		ResourceId resourceId = new ResourceId();
		resourceId.setKind("youtube#video");
		resourceId.setVideoId(videoId);

		// Set fields included in the playlistItem resource's "snippet" part.
		PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
		playlistItemSnippet.setTitle("Test video "
				+ Calendar.getInstance().getTime());
		playlistItemSnippet.setPlaylistId(playlistId);
		playlistItemSnippet.setResourceId(resourceId);

		// Create the playlistItem resource and set its snippet to the
		// object created above.
		PlaylistItem playlistItem = new PlaylistItem();
		playlistItem.setSnippet(playlistItemSnippet);

		// Call the API to add the playlist item to the specified playlist.
		// In the API call, the first argument identifies the resource parts
		// that the API response should contain, and the second argument is
		// the playlist item being inserted.
		YouTube.PlaylistItems.Insert playlistItemsInsertCommand = youtube
				.playlistItems().insert("snippet,contentDetails", playlistItem);
		PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand
				.execute();

		log.debug("New PlaylistItem name: "
				+ returnedPlaylistItem.getSnippet().getTitle());
		log.debug(" -Video id: "
				+ returnedPlaylistItem.getSnippet().getResourceId()
						.getVideoId());
		log.debug(" -Posted: "
				+ returnedPlaylistItem.getSnippet().getPublishedAt());
		log.debug(" -Channel: "
				+ returnedPlaylistItem.getSnippet().getChannelId());
		return returnedPlaylistItem.getId();
	}

	/**
	 * @param playlistItemId
	 * @throws IOException
	 */
	public static void removeItem(String playlistItemId) throws IOException {
		YouTube.PlaylistItems.Delete deleteCommand = youtube.playlistItems()
				.delete(playlistItemId);
		deleteCommand.execute();
	}

}
