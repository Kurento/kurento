/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.base;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.testing.SystemStabilityTests;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.utils.Shell;

/**
 * Stability tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
@Category(SystemStabilityTests.class)
public class StabilityTest extends BaseTest {

  private static final int WAIT_POLL_TIME = 200; // milliseconds

  public final long DEFAULT_TEST_DURATION = 300000; // ms

  public long endTestTime = 0;

  public StabilityTest() {
    setDeleteLogsIfSuccess(false);
  }

  public boolean isTimeToFinishTest() {
    return System.currentTimeMillis() > endTestTime;
  }

  public void testPlayerMultipleSeek(String mediaUrl, WebRtcChannel webRtcChannel,
      int pauseTimeSeconds, int numSeeks, Map<Integer, Color> expectedPositionAndColor)
      throws Exception {
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    final CountDownLatch flowingLatch = new CountDownLatch(1);

    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        log.debug("Received EndOfStream Event");
        eosLatch.countDown();
      }
    });

    webRtcEp.addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangedEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          flowingLatch.countDown();
        }
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.RCV_ONLY);
    playerEp.play();

    Assert.assertTrue(
        "Not received media (timeout waiting playing event): " + mediaUrl + " " + webRtcChannel,
        getPage().waitForEvent("playing"));

    Assert.assertTrue(
        "Not received FLOWING IN event in webRtcEp: " + mediaUrl + " " + webRtcChannel,
        flowingLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // TODO: Check with playerEP.getVideoInfo().getIsSeekable() if the video is seekable. If not,
    // assert with exception from KMS

    Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds));
    Integer executions = -1;
    for (int i = 0; i < numSeeks; i++) {
      executions++;
      log.debug("Try to set position in 0");
      playerEp.setPosition(0);
      for (Integer position : expectedPositionAndColor.keySet()) {
        executions++;
        log.debug("Try to set position in {}", position);
        playerEp.setPosition(position);
        if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
          boolean isSimilarColor = getPage().similarColor(expectedPositionAndColor.get(position));
          log.debug("Is the color of the video: {} ? {} ", expectedPositionAndColor.get(position),
              isSimilarColor);
          if (eosLatch.getCount() == 0) {
            break;
          }
          Assert.assertTrue("After set position to " + position
              + "ms, the color of the video should be " + expectedPositionAndColor.get(position),
              isSimilarColor);
        }
        // TODO: Add new method for checking that audio did pause properly when kurento-utils has
        // the
        // feature.
      }
      if (eosLatch.getCount() == 0) {
        break;
      }
    }

    Integer executionsExpected = (numSeeks * expectedPositionAndColor.size()) + numSeeks - 1;

    log.debug("The times executed. Expected  {}. Total {}.", executionsExpected, executions);
    Assert.assertTrue("The times executed is wrong. Minimun should be 1. Total: " + executions,
        (executions > 1));

    // Assertions

    Assert.assertTrue("Not received EOS event in player: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Release Media Pipeline
    playerEp.release();
    mp.release();
  }

  protected void waitForFileExists(String recordingFile) {
    boolean exists = false;
    String pathToMedia_[] = recordingFile.split("://");

    String protocol = "";
    String path = "";

    if (pathToMedia_.length > 1) {
      protocol = pathToMedia_[0];
      path = pathToMedia_[1];
    } else {
      String recordDefaultPath = KurentoTest.getRecordDefaultPath();

      if (recordDefaultPath != null) {
        String defaultPathToMedia_[] = recordDefaultPath.split("://");
        protocol = defaultPathToMedia_[0];
        String pathStart = defaultPathToMedia_[1];

        path = pathStart + pathToMedia_[0];
      }
    }

    log.debug("Waiting for the file to be saved: {}", recordingFile);

    long timeoutMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(WAIT_POLL_TIME);
    do {

      if (Protocol.FILE.toString().equals(protocol)) {
        String output = Shell.runAndWaitString("ls " + path);
        if (!output.contains("No such file or directory")) {
          exists = true;
        }
      } else if (Protocol.HTTP.toString().equals(protocol)
          || Protocol.HTTPS.toString().equals(protocol)) {
        exists = true;
      } else if (Protocol.S3.toString().equals(protocol)) {
        recordingFile = protocol + "://" + path;
        String output = Shell.runAndWaitString("aws s3 ls " + recordingFile);
        if (!output.equals("")) {
          exists = true;
        }
      } else if (Protocol.MONGODB.toString().equals(protocol)) {
        // TODO
      }

      if (!exists) {

        // Check timeout
        if (System.currentTimeMillis() > timeoutMs) {
          throw new KurentoException(
              "Timeout of " + WAIT_POLL_TIME + " seconds waiting for file: " + recordingFile);
        }

        try {
          // Wait WAIT_HUB_POLL_TIME ms
          log.debug("File {} does not exist ... waiting {} ms", recordingFile, WAIT_POLL_TIME);
          Thread.sleep(WAIT_POLL_TIME);

        } catch (InterruptedException e) {
          log.error("Exception waiting for recording file");
        }

      }
    } while (!exists);
  }

}
