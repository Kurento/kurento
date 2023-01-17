/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.functional.recorder;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.kurento.client.Continuation;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.Protocol;
import org.kurento.test.mediainfo.AssertMedia;
import org.kurento.test.utils.CheckAudioTimerTask;
import org.kurento.test.utils.Shell;

/**
 * Base for recorder tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class BaseRecorder extends FunctionalTest {

  public static final String EXPECTED_VIDEO_CODEC_WEBM = "VP8";
  public static final String EXPECTED_VIDEO_CODEC_MP4 = "AVC";
  public static final String EXPECTED_AUDIO_CODEC_WEBM = "Opus";
  public static final String EXPECTED_AUDIO_CODEC_MP4 = "MPEG Audio";
  public static final String EXTENSION_WEBM = ".webm";
  public static final String EXTENSION_MP4 = ".mp4";

  private static final int WAIT_POLL_TIME = 200; // milliseconds

  protected boolean success = false;
  protected String gstreamerDot;
  protected String pipelineName;

  @After
  public void storeGStreamerDot() throws IOException {
    if (!success && gstreamerDot != null) {
      String gstreamerDotFile = getDefaultOutputFile("-before-stop-recording-" + pipelineName);
      FileUtils.writeStringToFile(new File(gstreamerDotFile), gstreamerDot);
    }
  }

  protected void launchBrowser(MediaPipeline mp, WebRtcEndpoint webRtcEp, PlayerEndpoint playerEp,
      RecorderEndpoint recorderEp, String expectedVideoCodec, String expectedAudioCodec,
      String recordingFile, Color expectedColor, int xColor, int yColor, int playTime)
          throws InterruptedException {

    Timer gettingStats = new Timer();
    final CountDownLatch errorContinuityAudiolatch = new CountDownLatch(1);

    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    playerEp.play();
    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });

    if (recorderEp != null) {
      recorderEp.record();
    }

    // Assertions
    String inRecording = recorderEp == null ? " in the recording" : "";

    Assert.assertTrue("Not received media (timeout waiting playing event)" + inRecording,
        getPage().waitForEvent("playing"));

    if (recorderEp == null) {
      // Checking continuity of the audio
      getPage().activatePeerConnectionInboundStats("webRtcPeer.peerConnection");

      gettingStats.schedule(new CheckAudioTimerTask(errorContinuityAudiolatch, getPage()), 100,
          200);
    }

    Assert.assertTrue(
        "Color at coordinates " + xColor + "," + yColor + " must be " + expectedColor + inRecording,
        getPage().similarColorAt(expectedColor, xColor, yColor));
    Assert.assertTrue("Not received EOS event in player" + inRecording,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    final CountDownLatch recorderLatch = new CountDownLatch(1);
    if (recorderEp != null) {

      saveGstreamerDot(mp);

      recorderEp.stopAndWait(new Continuation<Void>() {

        @Override
        public void onSuccess(Void result) throws Exception {
          recorderLatch.countDown();
        }

        @Override
        public void onError(Throwable cause) throws Exception {
          recorderLatch.countDown();
        }
      });

      Assert.assertTrue("Not stop properly",
          recorderLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

      // Wait until file exists
      waitForFileExists(recordingFile);

      AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
      AssertMedia.assertDuration(recordingFile, TimeUnit.SECONDS.toMillis(playTime),
          TimeUnit.SECONDS.toMillis(getPage().getThresholdTime()));

    } else {
      gettingStats.cancel();
      getPage().stopPeerConnectionInboundStats("webRtcPeer.peerConnection");
      double currentTime = getPage().getCurrentTime();
      Assert.assertTrue("Error in play time in the recorded video (expected: " + playTime
          + " sec, real: " + currentTime + " sec) " + inRecording,
          getPage().compare(playTime, currentTime));

      if (recorderEp == null) {
        Assert.assertTrue("Check audio. There were more than 2 seconds without receiving packets",
            errorContinuityAudiolatch.getCount() == 1);
      }

    }
  }

  protected void saveGstreamerDot(MediaPipeline mp) {
    if (mp != null) {
      gstreamerDot = mp.getGstreamerDot();
      pipelineName = mp.getName();
    }
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

  protected void checkRecordingFile(String recordingFile, String browserName,
      Color[] expectedColors, long playTime, String expectedVideoCodec, String expectedAudioCodec)
          throws InterruptedException {

    // Checking continuity of the audio
    Timer gettingStats = new Timer();
    final CountDownLatch errorContinuityAudiolatch = new CountDownLatch(1);

    waitForFileExists(recordingFile);

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, recordingFile).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    // Playing the recording
    WebRtcTestPage checkPage = getPage(browserName);
    checkPage.setThresholdTime(checkPage.getThresholdTime() * 2);
    checkPage.subscribeEvents("playing");
    checkPage.initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY);
    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });
    playerEp.play();

    // Assertions in recording
    final String messageAppend = "[played file with media pipeline]";
    Assert.assertTrue(
        "Not received media in the recording (timeout waiting playing event) " + messageAppend,
        checkPage.waitForEvent("playing"));

    checkPage.activatePeerConnectionInboundStats("webRtcPeer.peerConnection");

    gettingStats.schedule(new CheckAudioTimerTask(errorContinuityAudiolatch, checkPage), 100, 200);

    for (Color color : expectedColors) {
      Assert.assertTrue("The color of the recorded video should be " + color + " " + messageAppend,
          checkPage.similarColorAt(color, 50, 50));
    }
    Assert.assertTrue("Not received EOS event in player",
        eosLatch.await(checkPage.getTimeout(), TimeUnit.SECONDS));

    gettingStats.cancel();

    double currentTime = checkPage.getCurrentTime();
    Assert.assertTrue("Error in play time in the recorded video (expected: " + playTime
        + " sec, real: " + currentTime + " sec) " + messageAppend,
        checkPage.compare(playTime, currentTime));

    Assert.assertTrue("Check audio. There were more than 2 seconds without receiving packets",
        errorContinuityAudiolatch.getCount() == 1);

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    AssertMedia.assertDuration(recordingFile, TimeUnit.SECONDS.toMillis(playTime),
        TimeUnit.SECONDS.toMillis(checkPage.getThresholdTime()));

    mp.release();
  }

}
