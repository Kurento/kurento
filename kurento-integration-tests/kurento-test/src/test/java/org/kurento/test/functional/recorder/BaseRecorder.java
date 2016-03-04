/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.functional.recorder;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
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
import org.kurento.test.config.Protocol;
import org.kurento.test.mediainfo.AssertMedia;
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
    Assert.assertTrue(
        "Color at coordinates " + xColor + "," + yColor + " must be " + expectedColor + inRecording,
        getPage().similarColorAt(expectedColor, xColor, yColor));
    Assert.assertTrue("Not received EOS event in player" + inRecording,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
    if (recorderEp != null) {

      saveGstreamerDot(mp);

      recorderEp.stop();

      // Wait until file exists
      waitForFileExists(recordingFile);

      AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
      AssertMedia.assertDuration(recordingFile, TimeUnit.SECONDS.toMillis(playTime),
          TimeUnit.SECONDS.toMillis(getPage().getThresholdTime()));

    } else {
      double currentTime = getPage().getCurrentTime();
      Assert.assertTrue("Error in play time in the recorded video (expected: " + playTime
          + " sec, real: " + currentTime + " sec) " + inRecording,
          getPage().compare(playTime, currentTime));
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
          throw new KurentoException("Timeout of " + WAIT_POLL_TIME + " seconds waiting for file: "
              + recordingFile);
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
