/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.RepositoryApiTests;
import org.kurento.repository.OneRecordingServer;
import org.kurento.repository.test.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Category(RepositoryApiTests.class)
public class OneRecordingServerTest {

  private static final Logger log = LoggerFactory.getLogger(OneRecordingServerTest.class);

  @Before
  public void setUp() {
    OneRecordingServer.startServerAndWait();
  }

  @After
  public void tearDown() {
    OneRecordingServer.stop();
  }

  @Test
  public void test() throws Exception {

    String publicWebappURL = OneRecordingServer.getPublicWebappUrl();

    log.debug("Start uploading content");

    File fileToUpload = new File("test-files/logo.png");

    uploadFileWithCURL(publicWebappURL + "repository_servlet/video-upload", fileToUpload);

    log.debug("Waiting 10 seconds to auto-termination...");
    Thread.sleep(10 * 1000);

    File downloadedFile = new File("test-files/sampleDownload.txt");

    log.debug("Start downloading file");
    downloadFromURL(publicWebappURL + "repository_servlet/video-download", downloadedFile);

    boolean equalFiles = TestUtils.equalFiles(fileToUpload, downloadedFile);

    if (equalFiles) {
      log.debug("The uploadad and downloaded files are equal");
    } else {
      log.debug("The uploadad and downloaded files are different");
    }

    assertTrue("The uploadad and downloaded files are different", equalFiles);
  }

  protected void downloadFromURL(String urlToDownload, File downloadedFile) throws Exception {

    if (!downloadedFile.exists()) {
      downloadedFile.createNewFile();
    }

    log.debug(urlToDownload);

    RestTemplate client = new RestTemplate();
    ResponseEntity<byte[]> response = client.getForEntity(urlToDownload, byte[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    FileOutputStream os = new FileOutputStream(downloadedFile);
    os.write(response.getBody());
    os.close();
  }

  protected void uploadFileWithCURL(String uploadURL, File fileToUpload)
      throws FileNotFoundException, IOException {

    log.debug("Start uploading file with curl");
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
    log.debug("Finished uploading content in " + (double) duration / 1000 + " seconds.");
  }

  protected void uploadFileWithPOST(String uploadURL, File fileToUpload)
      throws FileNotFoundException, IOException {

    RestTemplate template = new RestTemplate();

    ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
    IOUtils.copy(new FileInputStream(fileToUpload), fileBytes);

    ResponseEntity<String> entity =
        template.postForEntity(uploadURL, fileBytes.toByteArray(), String.class);

    assertEquals("Returned response: " + entity.getBody(), HttpStatus.OK, entity.getStatusCode());

  }
}