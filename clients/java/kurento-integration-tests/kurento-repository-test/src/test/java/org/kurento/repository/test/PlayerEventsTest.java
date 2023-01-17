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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.repository.HttpSessionStartedEvent;
import org.kurento.repository.HttpSessionTerminatedEvent;
import org.kurento.repository.RepositoryHttpEventListener;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.test.util.BaseRepositoryTest;
import org.kurento.repository.test.util.TestUtils;

public class PlayerEventsTest extends BaseRepositoryTest {

  @Test
  public void testFileUploadAndDownload() throws Exception {

    RepositoryItem repositoryItem = getRepository().createRepositoryItem();

    String id = repositoryItem.getId();

    File fileToUpload = new File("test-files/sample.txt");

    uploadWithEvents(repositoryItem, fileToUpload);

    File downloadedFile = downloadWithEvents(id);

    assertTrue("The uploaded file and the result of download it again are different",
        TestUtils.equalFiles(fileToUpload, downloadedFile));
  }

  private void uploadWithEvents(RepositoryItem repositoryItem, File fileToUpload)
      throws URISyntaxException, FileNotFoundException, IOException, InterruptedException {
    RepositoryHttpRecorder recorder = repositoryItem.createRepositoryHttpRecorder();

    final CountDownLatch started = new CountDownLatch(1);
    recorder.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
      @Override
      public void onEvent(HttpSessionStartedEvent event) {
        started.countDown();
      }
    });

    final CountDownLatch terminated = new CountDownLatch(1);
    recorder.addSessionTerminatedListener(
        new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
          @Override
          public void onEvent(HttpSessionTerminatedEvent event) {
            terminated.countDown();
          }
        });

    uploadFileWithPOST(recorder.getURL(), fileToUpload);

    // TODO We need to be sure that this events appear in the order
    // specified. This test doesn't control this

    assertTrue("Started event didn't sent in 10 seconds", started.await(10, TimeUnit.SECONDS));
    assertTrue("Terminated event didn't sent in 10 seconds",
        terminated.await(10, TimeUnit.SECONDS));
  }

  private File downloadWithEvents(String id) throws Exception, InterruptedException {

    RepositoryItem newRepositoryItem = getRepository().findRepositoryItemById(id);

    RepositoryHttpPlayer player = newRepositoryItem.createRepositoryHttpPlayer();

    final CountDownLatch started = new CountDownLatch(1);
    player.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
      @Override
      public void onEvent(HttpSessionStartedEvent event) {
        started.countDown();
      }
    });

    final CountDownLatch terminated = new CountDownLatch(1);
    player.addSessionTerminatedListener(
        new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
          @Override
          public void onEvent(HttpSessionTerminatedEvent event) {
            terminated.countDown();
          }
        });

    File downloadedFile = new File("test-files/tmp/" + id);
    downloadFromURL(player.getURL(), downloadedFile);

    // TODO We need to be sure that this events appear in the order
    // specified. This test doesn't control this

    assertTrue("Started event didn't sent in 10 seconds", started.await(10, TimeUnit.SECONDS));
    assertTrue("Terminated event didn't sent in 10 seconds",
        terminated.await(10, TimeUnit.SECONDS));
    return downloadedFile;
  }

}