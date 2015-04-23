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

package org.kurento.repository.test.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Before;
import org.kurento.repository.Repository;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class HttpRepositoryTest extends ContextByTestSpringBootTest {

	private static final Logger log = LoggerFactory
			.getLogger(HttpRepositoryTest.class);

	public HttpRepositoryTest() {
		super();
	}

	@Before
	public void cleanTmp() {
		File tmpFolder = new File("test-files/tmp");

		tmpFolder.delete();
		tmpFolder.mkdirs();

		Repository repo = getRepository();
		if (repo instanceof MongoRepository) {
			MongoRepository mrepo = (MongoRepository) repo;
			mrepo.getGridFS().getDB().dropDatabase();
		}
	}

	protected Repository getRepository() {
		return (Repository) context.getBean("repository");
	}

	protected void downloadFromURL(String urlToDownload, File downloadedFile)
			throws Exception {

		RestTemplate template = getRestTemplate();
		ResponseEntity<byte[]> entity = template.getForEntity(urlToDownload,
				byte[].class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());

		FileOutputStream os = new FileOutputStream(downloadedFile);
		os.write(entity.getBody());
		os.close();
	}

	protected File downloadFromRepoItemId(String id) throws Exception {

		RepositoryItem newRepositoryItem = getRepository()
				.findRepositoryItemById(id);

		RepositoryHttpPlayer player = newRepositoryItem
				.createRepositoryHttpPlayer();

		File downloadedFile = new File("test-files/tmp/" + id);

		if (downloadedFile.exists()) {
			boolean success = downloadedFile.delete();
			if (!success) {
				throw new RuntimeException("The existing file "
						+ downloadedFile + " cannot be deleted");
			}
		}

		downloadFromURL(player.getURL(), downloadedFile);

		return downloadedFile;
	}

	protected void uploadFileWithMultiparts(String uploadURL, File fileToUpload) {

		RestTemplate template = getRestTemplate();

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add("file", new FileSystemResource(fileToUpload));

		ResponseEntity<String> entity = postWithRetries(uploadURL, template,
				parts);

		assertEquals("Returned response: " + entity.getBody(), HttpStatus.OK,
				entity.getStatusCode());
	}

	protected void uploadFileWithPOST(String uploadURL, File fileToUpload)
			throws FileNotFoundException, IOException {

		RestTemplate client = getRestTemplate();

		ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
		IOUtils.copy(new FileInputStream(fileToUpload), fileBytes);

		ResponseEntity<String> entity = postWithRetries(uploadURL, client,
				fileBytes.toByteArray());

		log.info("Upload response");

		assertEquals("Returned response: " + entity.getBody(), HttpStatus.OK,
				entity.getStatusCode());
	}

	protected String uploadFile(File fileToUpload)
			throws FileNotFoundException, IOException {

		RepositoryItem repositoryItem = getRepository().createRepositoryItem();
		return uploadFile(fileToUpload, repositoryItem);
	}

	protected String uploadFile(File fileToUpload, RepositoryItem repositoryItem)
			throws FileNotFoundException, IOException {

		String id = repositoryItem.getId();

		RepositoryHttpRecorder recorder = repositoryItem
				.createRepositoryHttpRecorder();

		uploadFileWithMultiparts(recorder.getURL(), fileToUpload);

		recorder.stop();

		return id;
	}

	private ResponseEntity<String> postWithRetries(String uploadURL,
			RestTemplate template, Object request) {

		ResponseEntity<String> entity = null;

		int numRetries = 0;
		while (true) {
			try {
				entity = template.postForEntity(uploadURL, request,
						String.class);
				break;
			} catch (Exception e) {
				log.warn("Exception when uploading file with POST. Retring...");
				log.warn("Exception message: " + e.getMessage());
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
				numRetries++;
				if (numRetries > 5) {
					throw new RuntimeException(e);
				}
			}
		}
		return entity;
	}

}