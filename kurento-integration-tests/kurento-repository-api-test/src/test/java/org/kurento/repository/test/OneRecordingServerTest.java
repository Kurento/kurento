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

package org.kurento.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.kurento.commons.tests.RepositoryApiTests;
import org.kurento.repository.main.OneRecordingServer;
import org.kurento.repository.test.util.TestUtils;

@Category(RepositoryApiTests.class)
public class OneRecordingServerTest {

	private static final Logger log = LoggerFactory
			.getLogger(OneRecordingServerTest.class);

	@Test
	public void test() throws Exception {

		OneRecordingServer.startServerAndWait();

		String publicWebappURL = OneRecordingServer.getPublicWebappURL();

		log.info("Start uploading content");

		File fileToUpload = new File("test-files/logo.png");

		uploadFileWithCURL(publicWebappURL + "repository_servlet/video-upload",
				fileToUpload);

		log.info("Waiting 6 seconds to auto-termination...");
		Thread.sleep(6 * 1000);

		File downloadedFile = new File("test-files/sampleDownload.txt");

		log.info("Start downloading file");
		downloadFromURL(publicWebappURL + "repository_servlet/video-download",
				downloadedFile);

		boolean equalFiles = TestUtils.equalFiles(fileToUpload, downloadedFile);

		if (equalFiles) {
			log.info("The uploadad and downloaded files are equal");
		} else {
			log.info("The uploadad and downloaded files are different");
		}

		assertTrue("The uploadad and downloaded files are different",
				equalFiles);

		OneRecordingServer.stop();
	}

	protected void downloadFromURL(String urlToDownload, File downloadedFile)
			throws Exception {

		if (!downloadedFile.exists()) {
			downloadedFile.createNewFile();
		}

		log.info(urlToDownload);

		RestTemplate client = new RestTemplate();
		ResponseEntity<byte[]> response = client.getForEntity(urlToDownload,
				byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());

		FileOutputStream os = new FileOutputStream(downloadedFile);
		os.write(response.getBody());
		os.close();
	}

	protected void uploadFileWithCURL(String uploadURL, File fileToUpload)
			throws FileNotFoundException, IOException {

		log.info("Start uploading file with curl");
		long startTime = System.currentTimeMillis();

		ProcessBuilder builder = new ProcessBuilder("curl", "-i", "-F",
				"filedata=@" + fileToUpload.getAbsolutePath(), uploadURL);
		builder.redirectOutput();
		Process process = builder.start();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long duration = System.currentTimeMillis() - startTime;
		log.info("Finished uploading content in "
				+ (((double) duration) / 1000) + " seconds.");
	}

	protected void uploadFileWithPOST(String uploadURL, File fileToUpload)
			throws FileNotFoundException, IOException {

		RestTemplate template = new RestTemplate();

		ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
		IOUtils.copy(new FileInputStream(fileToUpload), fileBytes);

		ResponseEntity<String> entity = template.postForEntity(uploadURL,
				fileBytes.toByteArray(), String.class);

		assertEquals("Returned response: " + entity.getBody(), HttpStatus.OK,
				entity.getStatusCode());

	}
}