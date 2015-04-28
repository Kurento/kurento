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

package org.kurento.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kurento.commons.ConfigFileManager;
import org.kurento.repository.RepositoryApiConfiguration.RepoType;
import org.kurento.repository.internal.RepositoryApplicationContextConfiguration;
import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.kurento.repository.rest.RepositoryRestApi;
import org.kurento.repository.service.pojo.RepositoryItemPlayer;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Test for the {@link KurentoRepositoryServerApp}. Starts the target Spring app
 * and tests its REST endpoint(s). Can receive an option on the command line
 * {@code (testWithFS=true)} so that the repository is forced to use the
 * filesystem.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class RepositoryRestTest {

	private static final Logger log = LoggerFactory
			.getLogger(RepositoryRestTest.class);

	private RepositoryRestApi restService;

	private Repository repository;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	protected static ConfigurableApplicationContext app;

	@BeforeClass
	public static void setUpClass() {
		boolean testWithFS = getProperty("testWithFS", false);
		if (testWithFS) {
			System.setProperty(
					RepositoryApplicationContextConfiguration.KEY_REPO_TYPE,
					RepositoryApiConfiguration.RepoType.FILESYSTEM.getTypeValue());
			log.info("Filesystem has been forced as repo storage type");
		}
		app = KurentoRepositoryServerApp.start();
	}

	@AfterClass
	public static void tearDownClass() {
		app.close();
	}

	@Before
	public void setUp() {
		String serviceUrl = "http://"
				+ RepositoryApplicationContextConfiguration.SERVER_HOSTNAME
				+ ":" + RepositoryApplicationContextConfiguration.SERVER_PORT;
		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(
				serviceUrl).build();
		restService = restAdapter.create(RepositoryRestApi.class);
		log.info("Rest service created for {}", serviceUrl);

		repository = (Repository) app.getBean("repository");
		if (repository instanceof MongoRepository) {
			MongoRepository mrepo = (MongoRepository) repository;
			mrepo.getGridFS().getDB().dropDatabase();
			log.info("Cleaned up the Mongo repository");
		} else {
			ConfigFileManager
			.loadConfigFile(RepositoryApplicationContextConfiguration.KEY_CONFIG_FILENAME);

			String filesFolder = getProperty(
					RepositoryApplicationContextConfiguration.KEY_FS_FOLDER,
					RepositoryApiConfiguration.DEFAULT_FILESYSTEM_LOC);
			File fsFolder = new File(filesFolder);
			if (fsFolder.exists() && fsFolder.isDirectory())
				for (File child : fsFolder.listFiles())
					if (child.isFile())
						child.delete();
			log.info("Cleaned up the disk repository: {}", fsFolder);
		}
		File tmpFolder = new File("test-files/tmp");
		tmpFolder.delete();
		tmpFolder.mkdirs();
	}

	@Test
	public void test() throws FileNotFoundException, IOException,
	InterruptedException {
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("restKey", "restValue");
		RepositoryItemRecorder itemRec = restService
				.createRepositoryItem(metadata);
		log.info("Obtained item store: {}", itemRec);

		File fileToUpload = new File("test-files/logo.png");
		uploadFileWithCURL(itemRec.getUrl(), fileToUpload);

		Thread.sleep(1000 * 6);

		RepositoryItemPlayer itemPlay = restService.getReadEndpoint(itemRec
				.getId());
		assertEquals("Items' ids don't match", itemRec.getId(),
				itemPlay.getId());

		String playUrl = itemPlay.getUrl();
		File downloadedFile = new File("test-files/tmp/sampleDownload.txt");
		log.info("Start downloading file from {} to {}", playUrl,
				downloadedFile.getPath());
		downloadFromURL(playUrl, downloadedFile);

		boolean equalFiles = TestUtils.equalFiles(fileToUpload, downloadedFile);

		if (equalFiles) {
			log.info("The uploadad and downloaded files are equal");
		} else {
			log.info("The uploadad and downloaded files are different");
		}

		assertTrue("The uploadad and downloaded files are different",
				equalFiles);

		Set<String> items = restService.simpleFindItems(metadata);
		assertEquals(
				"Not one exact element found based on our simple search data: "
						+ metadata, 1, items.size());

		assertEquals("Ids don't match", itemRec.getId(), items.iterator()
				.next());

		if (RepoType.parseType(
				RepositoryApplicationContextConfiguration.REPO_TYPE)
				.isMongoDB()) {
			Map<String, String> regexValues = new HashMap<String, String>();
			regexValues.put("restKey", "restVal*");

			items = restService.regexFindItems(regexValues);
			assertEquals(
					"Not one exact element found based on our regex search data: "
							+ regexValues, 1, items.size());

			assertEquals("Ids don't match", itemRec.getId(), items.iterator()
					.next());
		}

		Map<String, String> serverMetadata = restService
				.getRepositoryItemMetadata(itemRec.getId());
		assertEquals("Local metadata doesn't match saved metadata", metadata,
				serverMetadata);

		Map<String, String> updateMetadata = new HashMap<String, String>();
		updateMetadata.put("restKey", "newVal");
		Response response = restService.setRepositoryItemMetadata(
				itemRec.getId(), updateMetadata);
		assertEquals(
				"Response status of metadata update request is not 200 OK",
				HttpStatus.OK.value(), response.getStatus());

		serverMetadata = restService.getRepositoryItemMetadata(itemRec.getId());
		assertEquals(
				"New local metadata doesn't match updated server metadata",
				updateMetadata, serverMetadata);

		response = restService.removeRepositoryItem(itemRec.getId());
		assertEquals("Response status of remove item request is not 200 OK",
				HttpStatus.OK.value(), response.getStatus());

		exception.expect(RetrofitError.class);
		exception.expectMessage(CoreMatchers.containsString("404 Not Found"));
		RepositoryItemPlayer itemFound = restService.getReadEndpoint(itemRec
				.getId());
		Assert.assertNull(itemFound);

		items = restService.simpleFindItems(serverMetadata);
		assertEquals(
				"No items should have been found based on our simple search data: "
						+ serverMetadata + " (was deleted)", 0, items.size());

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

	protected void downloadFromURL(String urlToDownload, File downloadedFile)
			throws IOException {

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
}
