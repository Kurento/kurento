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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.repository.HttpSessionErrorEvent;
import org.kurento.repository.HttpSessionStartedEvent;
import org.kurento.repository.RepositoryHttpEventListener;
import org.kurento.repository.RepositoryHttpRecorder;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.test.util.BaseRepositoryTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorEventsTest extends BaseRepositoryTest {

  private static final Logger log = LoggerFactory.getLogger(ErrorEventsTest.class);

  @Test
  public void testFileUploadAndDownload() throws Exception {

    RepositoryItem item = getRepository().createRepositoryItem();

    final RepositoryHttpRecorder recorder = item.createRepositoryHttpRecorder();

    final CountDownLatch started = new CountDownLatch(1);
    recorder.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
      @Override
      public void onEvent(HttpSessionStartedEvent event) {
        started.countDown();
      }
    });

    final CountDownLatch errorLatch = new CountDownLatch(1);
    recorder.addSessionErrorListener(new RepositoryHttpEventListener<HttpSessionErrorEvent>() {
      @Override
      public void onEvent(HttpSessionErrorEvent event) {
        log.debug("Error event sent");
        log.debug("Exception:" + event.getCause());
        errorLatch.countDown();
      }
    });

    log.debug(
        "Start writing to URL " + recorder.getURL() + " the item with id '" + item.getId() + "'");

    new Thread() {
      @Override
      public void run() {
        try {
          uploadFileWithPOST(recorder.getURL(), new File("test-files/logo.png"));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }.start();

    started.await();

    // Sleep to give time to open the outputStream to write the uploading
    // file.
    Thread.sleep(2000);

    getRepository().remove(item);

    assertTrue("Error event was not fired in the next 5 seconds before deletion of the file",
        errorLatch.await(5, TimeUnit.SECONDS));

  }

}