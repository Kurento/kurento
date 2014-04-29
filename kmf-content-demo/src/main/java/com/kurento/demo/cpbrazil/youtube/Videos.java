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

import static com.google.common.collect.Lists.newArrayList;
import static com.kurento.demo.cpbrazil.youtube.Auth.HTTP_TRANSPORT;
import static com.kurento.demo.cpbrazil.youtube.Auth.JSON_FACTORY;
import static com.kurento.demo.cpbrazil.youtube.Auth.authorise;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * Class with static methods implementing the upload to of a given URL to
 * YouTube.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.1
 * @see https://code.google.com/p/youtube-api-samples
 * 
 */
public class Videos {

	private static final Logger log = LoggerFactory.getLogger(Videos.class);

	/** Global instance of Youtube object to make all API requests. */
	private static YouTube youtube;

	static {
		// Scope required to upload to YouTube.
		List<String> scopes = newArrayList(
				"https://www.googleapis.com/auth/youtube",
				"https://www.googleapis.com/auth/youtube.upload");

		try {
			File credentialStoreFile = Auth.inputStreamToFile(Videos.class
					.getResourceAsStream("/youtube-api-uploadvideo.json"));

			Credential credential = authorise(scopes, credentialStoreFile);
			// YouTube object used to make all API requests.
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					credential).setApplicationName("kurento-videos").build();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException();
		}
	}

	public static Video upload(String url, String playListToken,
			List<String> tags) {
		Video uploadedVideo;

		try {
			VideoSnippet snippet = new VideoSnippet();

			Calendar cal = Calendar.getInstance();
			snippet.setTitle("FI-WARE project. Kurento Demo on Campus Party Brazil "
					+ cal.getTime());
			snippet.setDescription("Kurento demo  on " + cal.getTime());
			snippet.setTags(tags);

			Video video = new Video();
			video.setSnippet(snippet);

			URL website = new URL(url);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			String tmpFileName = "tmp";
			FileOutputStream fos = new FileOutputStream(tmpFileName);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			final File videoFile = new File(tmpFileName);
			InputStreamContent mediaContent = new InputStreamContent("video/*",
					new BufferedInputStream(new FileInputStream(videoFile)));

			/*
			 * The upload command includes: 1. Information we want returned
			 * after file is successfully uploaded. 2. Metadata we want
			 * associated with the uploaded video. 3. Video file itself.
			 */
			YouTube.Videos.Insert videoInsert = youtube.videos().insert(
					"snippet,statistics,status", video, mediaContent);

			// Set the upload type and add event listener.
			MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
			uploader.setDirectUploadEnabled(false);

			MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
				@Override
				public void progressChanged(MediaHttpUploader uploader)
						throws IOException {
					switch (uploader.getUploadState()) {
					case INITIATION_STARTED:
						log.debug("Initiation Started");
						break;
					case INITIATION_COMPLETE:
						log.debug("Initiation Completed");
						break;
					case MEDIA_IN_PROGRESS:
						log.debug("Upload in progress");
						log.debug("Upload percentage: "
								+ uploader.getProgress());
						break;
					case MEDIA_COMPLETE:
						log.debug("Upload Completed!");
						log.debug("Deleting local file...  "
								+ videoFile.delete());
						break;
					case NOT_STARTED:
						log.debug("Upload Not Started!");
						break;
					}
				}
			};
			uploader.setProgressListener(progressListener);

			uploadedVideo = videoInsert.execute();
			String playListItemId = Playlists.insertItem(playListToken,
					uploadedVideo.getId());

			// Print out returned results.
			log.info("\n================== Returned Video ==================\n");
			log.info(" -Id: " + uploadedVideo.getId());
			log.info(" -PlayList Item Id: " + playListItemId);
			log.info(" -Title: " + uploadedVideo.getSnippet().getTitle());
			log.info(" -Tags: " + uploadedVideo.getSnippet().getTags());
			log.info(" -Privacy Status: "
					+ uploadedVideo.getStatus().getPrivacyStatus());
			log.info(" -Video Count: "
					+ uploadedVideo.getStatistics().getViewCount());

		} catch (GoogleJsonResponseException e) {
			log.error("GoogleJsonResponseException code: "
					+ e.getDetails().getCode() + " : "
					+ e.getDetails().getMessage());
			throw new KurentoMediaFrameworkException(e);
		} catch (IOException e) {
			log.error("IOException: " + e.getMessage());
			throw new KurentoMediaFrameworkException(e);
		} catch (Throwable t) {
			log.error("Throwable: " + t.getMessage());
			throw new KurentoMediaFrameworkException(t);
		}

		return uploadedVideo;
	}

	public static void delete(String videoId) throws IOException {
		youtube.videos().delete(videoId).execute();
	}

	public static void delete(Video video) throws IOException {
		delete(video.getId());
	}
}
