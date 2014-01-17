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
package com.kurento.demo.cpbrazil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.collect.Lists;

public class UploadVideoYouTube {

	private static final Logger log = LoggerFactory
			.getLogger(UploadVideoYouTube.class);

	/** Global instance of the HTTP transport. */
	private final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** Global instance of the JSON factory. */
	private final JsonFactory JSON_FACTORY = new JacksonFactory();

	/** Global instance of Youtube object to make all API requests. */
	private YouTube youtube;

	/**
	 * Authorizes the installed application to access user's protected data.
	 * 
	 * @param scopes
	 *            list of scopes needed to run youtube upload.
	 */
	private Credential authorize(List<String> scopes) throws Exception {
		// Load client secrets.
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY, UploadVideoYouTube.class
						.getResourceAsStream("/client_secrets.json"));

		File credentialStoreFile = inputStreamToFile(UploadVideoYouTube.class
				.getResourceAsStream("/youtube-api-uploadvideo.json"));
		// Set up file credential store.
		FileCredentialStore credentialStore = new FileCredentialStore(
				credentialStoreFile, JSON_FACTORY);

		credentialStoreFile.delete();

		// Set up authorization code flow.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
				.setCredentialStore(credentialStore).build();

		// Build the local server and bind it to port 9090
		LocalServerReceiver localReceiver = new LocalServerReceiver.Builder()
				.setPort(9090).build();

		// Authorize.
		return new AuthorizationCodeInstalledApp(flow, localReceiver)
				.authorize("user");
	}

	public File inputStreamToFile(InputStream inputStream) throws IOException {
		File file = new File("credentialStore");
		OutputStream output = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while ((len = inputStream.read(buf)) > 0) {
			output.write(buf, 0, len);
		}
		output.close();
		inputStream.close();
		return file;
	}

	public void uploadVideo(String url) {
		// Scope required to upload to YouTube.
		List<String> scopes = Lists
				.newArrayList("https://www.googleapis.com/auth/youtube.upload");

		try {
			// Authorization.
			Credential credential = authorize(scopes);

			// YouTube object used to make all API requests.
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					credential).setApplicationName(
					"youtube-cmdline-uploadvideo-sample").build();

			// We get the user selected local video file to upload.
			// File videoFile = new File(
			// "/Users/boni/GSyC/media/selection/small.webm");

			// Add extra information to the video before uploading.
			Video videoObjectDefiningMetadata = new Video();

			/*
			 * Set the video to public, so it is available to everyone (what
			 * most people want). This is actually the default, but I wanted you
			 * to see what it looked like in case you need to set it to
			 * "unlisted" or "private" via API.
			 */
			VideoStatus status = new VideoStatus();
			status.setPrivacyStatus("public");
			videoObjectDefiningMetadata.setStatus(status);

			// We set a majority of the metadata with the VideoSnippet object.
			VideoSnippet snippet = new VideoSnippet();

			/*
			 * The Calendar instance is used to create a unique name and
			 * description for test purposes, so you can see multiple files
			 * being uploaded. You will want to remove this from your project
			 * and use your own standard names.
			 */
			Calendar cal = Calendar.getInstance();
			snippet.setTitle("FI-WARE project. Kurento Demo on Campus Party Brazil "
					+ cal.getTime());
			snippet.setDescription("Kurento demo  on " + cal.getTime());

			// Set your keywords.
			List<String> tags = new ArrayList<String>();
			tags.add("FI-WARE");
			tags.add("Kurento");
			tags.add("FUN-LAB");
			tags.add("GSyC");
			tags.add("URJC");
			tags.add("Campus Party");
			tags.add("WebRTC");
			tags.add("Software Engineering");
			tags.add("Augmented Reality");
			tags.add("Computer Vision");
			tags.add("Super Mario");
			tags.add("Sonic");
			tags.add("Street Fighter");
			tags.add("Donkey Kong");
			snippet.setTags(tags);

			// Set completed snippet to the video object.
			videoObjectDefiningMetadata.setSnippet(snippet);

			// InputStream inputStream = new URL(url).openStream();
			// log.info("*************** " + inputStream.available());
			// InputStreamContent mediaContent = new
			// InputStreamContent("video/*",
			// new BufferedInputStream(inputStream));
			// // mediaContent.setLength(inputStream.available());

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
					"snippet,statistics,status", videoObjectDefiningMetadata,
					mediaContent);

			// Set the upload type and add event listener.
			MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

			/*
			 * Sets whether direct media upload is enabled or disabled. True =
			 * whole media content is uploaded in a single request. False
			 * (default) = resumable media upload protocol to upload in data
			 * chunks.
			 */
			uploader.setDirectUploadEnabled(false);

			MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
				public void progressChanged(MediaHttpUploader uploader)
						throws IOException {
					switch (uploader.getUploadState()) {
					case INITIATION_STARTED:
						log.info("Initiation Started");
						break;
					case INITIATION_COMPLETE:
						log.info("Initiation Completed");
						break;
					case MEDIA_IN_PROGRESS:
						log.info("Upload in progress");
						log.info("Upload percentage: " + uploader.getProgress());
						break;
					case MEDIA_COMPLETE:
						log.info("Upload Completed!");
						log.info("Deleting tmp file...  " + videoFile.delete());
						break;
					case NOT_STARTED:
						log.info("Upload Not Started!");
						break;
					}
				}
			};
			uploader.setProgressListener(progressListener);

			// Execute upload.
			Video returnedVideo = videoInsert.execute();

			// Print out returned results.
			log.info("\n================== Returned Video ==================\n");
			log.info("  - Id: " + returnedVideo.getId());
			log.info("  - Title: " + returnedVideo.getSnippet().getTitle());
			log.info("  - Tags: " + returnedVideo.getSnippet().getTags());
			log.info("  - Privacy Status: "
					+ returnedVideo.getStatus().getPrivacyStatus());
			log.info("  - Video Count: "
					+ returnedVideo.getStatistics().getViewCount());

		} catch (GoogleJsonResponseException e) {
			log.error("GoogleJsonResponseException code: "
					+ e.getDetails().getCode() + " : "
					+ e.getDetails().getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("IOException: " + e.getMessage());
			e.printStackTrace();
		} catch (Throwable t) {
			log.error("Throwable: " + t.getMessage());
			t.printStackTrace();
		}
	}
}