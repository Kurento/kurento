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

package org.kurento.repository;

import java.util.concurrent.CountDownLatch;

import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class OneRecordingServer {

  private static final Logger log = LoggerFactory.getLogger(OneRecordingServer.class);

  private ConfigurableApplicationContext context;

  public void execute() throws Exception {

    startServer();

    RepositoryItem repositoryItem = getRepository().createRepositoryItem();
    Repository repo = getRepository();
    if (repo instanceof MongoRepository) {
      MongoRepository mrepo = (MongoRepository) repo;
      mrepo.getGridFS().getDB().dropDatabase();
    }

    prepareToUploadVideo(repositoryItem);
    prepareToDownloadVideo(repositoryItem);

    stopServer();
  }

  public synchronized void startServer() throws Exception {
    if (context == null) {
      context = KurentoRepositoryServerApp.start();
    }
  }

  private synchronized void stopServer() {

    if (context != null) {
      context.close();
      context = null;
    }
  }

  private void prepareToDownloadVideo(RepositoryItem repositoryItem) throws InterruptedException {
    RepositoryHttpPlayer player = repositoryItem.createRepositoryHttpPlayer("video-download");
    log.debug("The video can be downloaded with GET from the URL: " + player.getURL());

    player.setAutoTerminationTimeout(30 * 60 * 1000);
    log.debug("Player will be terminated 30 min after last download of content (http GET)");

    final CountDownLatch terminatedLatch = new CountDownLatch(1);

    player.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
      @Override
      public void onEvent(HttpSessionStartedEvent event) {
        log.debug("Downloading started");
      }
    });

    player.addSessionTerminatedListener(
        new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
          @Override
          public void onEvent(HttpSessionTerminatedEvent event) {
            log.debug("Downloading terminated");
            terminatedLatch.countDown();
          }
        });

    try {
      terminatedLatch.await();
    } catch (InterruptedException e) {
      // Intentionally left blank{
    }

  }

  private void prepareToUploadVideo(RepositoryItem repositoryItem) throws InterruptedException {

    RepositoryHttpRecorder recorder = repositoryItem.createRepositoryHttpRecorder("video-upload");

    log.debug("The video must be uploaded with PUT or POST to the URL: " + recorder.getURL());

    readyToUploadWatch.countDown();

    recorder.setAutoTerminationTimeout(5 * 1000);
    log.debug(
        "Recorder will be terminated 5 seconds after last upload of content (http PUT or POST)");

    final CountDownLatch terminatedLatch = new CountDownLatch(1);

    recorder.addSessionStartedListener(new RepositoryHttpEventListener<HttpSessionStartedEvent>() {
      @Override
      public void onEvent(HttpSessionStartedEvent event) {
        log.debug("Uploading started");
      }
    });

    recorder.addSessionTerminatedListener(
        new RepositoryHttpEventListener<HttpSessionTerminatedEvent>() {
          @Override
          public void onEvent(HttpSessionTerminatedEvent event) {
            log.debug("Uploading terminated");
            terminatedLatch.countDown();
          }
        });

    terminatedLatch.await();
  }

  protected Repository getRepository() {
    return (Repository) context.getBean("repository");
  }

  // Convenience static methods and attributes

  private static CountDownLatch readyToUploadWatch = new CountDownLatch(1);
  private static OneRecordingServer server;
  private static Thread thread;

  public static void main(String[] args) throws Exception {
    server = new OneRecordingServer();
    server.execute();
  }

  public static String getPublicWebappUrl() {
    String web = server.context.getBean(RepositoryApiConfiguration.class).getWebappPublicUrl();
    // String web = "http://localhost:8080/";

    log.debug("web: " + web);
    return web;
  }

  public static void startServerAndWait() {

    thread = new Thread() {
      @Override
      public void run() {
        try {
          OneRecordingServer.main(null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };

    thread.start();

    try {
      readyToUploadWatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void stop() {
    thread.interrupt();
    try {
      thread.join();
    } catch (InterruptedException e) {
      thread.interrupt();
    }
  }
}
