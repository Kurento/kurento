package org.kurento.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.kurento.repository.service.pojo.RepositoryItemStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = KurentoRepositoryServerApp.class)
@WebAppConfiguration
@IntegrationTest//("server.port:0")
public class RepositoryRestTest {

	private static final Logger log = LoggerFactory
			.getLogger(RepositoryRestTest.class);

	public interface RepositoryRestApi {

		@POST("/repo/item")
		RepositoryItemStore createRepositoryItem(
				@Body Map<String, String> metadata);

		@GET("/repo/item/{itemId}/read")
		String getReadEndpoint(@Path("itemId") String itemId);

	}

	@Value("${local.server.port}")
	private int port;

	private RepositoryRestApi restService;

	@Autowired
	private Repository repository;

	@Before
	public void setUp() {
		String serviceUrl = "http://127.0.0.1:" + KurentoRepositoryServerApp.SERVER_PORT;
		RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(
				serviceUrl).build();
		restService = restAdapter.create(RepositoryRestApi.class);
		log.info("Rest service created for {}", serviceUrl);

		if (repository instanceof MongoRepository) {
			MongoRepository mrepo = (MongoRepository) repository;
			mrepo.getGridFS().getDB().dropDatabase();
		}
	}

	@Test
	public void test() throws FileNotFoundException, IOException,
	InterruptedException {
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("restKey", "restValue");
		RepositoryItemStore itemStore = restService
				.createRepositoryItem(metadata);
		log.info("Obtained item store: {}", itemStore);

		File fileToUpload = new File("test-files/logo.png");
		uploadFileWithCURL(itemStore.getUrl(), fileToUpload);

		Thread.sleep(1000 * 6);

		String playUrl = restService.getReadEndpoint(itemStore.getId());
		File downloadedFile = new File("test-files/sampleDownload.txt");

		log.info("Start downloading file");
		downloadFromURL(playUrl, downloadedFile);

		boolean equalFiles = TestUtils.equalFiles(fileToUpload, downloadedFile);

		if (equalFiles) {
			log.info("The uploadad and downloaded files are equal");
		} else {
			log.info("The uploadad and downloaded files are different");
		}

		assertTrue("The uploadad and downloaded files are different",
				equalFiles);
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
