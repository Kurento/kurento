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

package org.kurento.test.functional.player;

import java.awt.Color;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.PlayerTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.VideoFormat;
import org.kurento.test.utils.CheckAudioTimerTask;

/**
 * Base for player tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class SimplePlayer extends PlayerTest {

  public void testPlayerWithRtsp(WebRtcChannel webRtcChannel) throws Exception {
    getPage().getBrowser().setTimeout(200);
    testPlayer("rtsp://mm2.pcslab.com/mm/7m2000.mp4", webRtcChannel, 0, 50, 50, Color.BLACK);
  }

  public void testPlayerWithSmallFileVideoOnly(Protocol protocol, VideoFormat videoFormat,
      WebRtcChannel webRtcChannel) throws InterruptedException {
    testPlayerWithSmallFile(protocol, videoFormat, webRtcChannel, true);
  }

  public void testPlayerWithSmallFile(Protocol protocol, VideoFormat videoFormat,
      WebRtcChannel webRtcChannel) throws InterruptedException {
    testPlayerWithSmallFile(protocol, videoFormat, webRtcChannel, false);
  }

  private void testPlayerWithSmallFile(Protocol protocol, VideoFormat videoFormat,
      WebRtcChannel webRtcChannel, boolean videoOnly) throws InterruptedException {
    // Reduce threshold time per test
    getPage().setThresholdTime(5); // seconds

    String nameMedia = "/video/format/";
    nameMedia += videoOnly ? "small_video_only." : "small.";
    nameMedia += videoFormat.toString();

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    log.debug(">>>> Playing small video ({}) on {}", webRtcChannel, mediaUrl);
    testPlayer(mediaUrl, webRtcChannel, 5, 100, 100, new Color(128, 85, 46));
  }

  public void testPlayer(String mediaUrl, WebRtcChannel webRtcChannel, int playtime)
      throws InterruptedException {
    testPlayer(mediaUrl, webRtcChannel, playtime, 0, 0, null);
  }

  public void testPlayer(String mediaUrl, WebRtcChannel webRtcChannel, int playtime, int x, int y,
      Color expectedColor) throws InterruptedException {

    Timer gettingStats = new Timer();
    final CountDownLatch errorContinuityAudiolatch = new CountDownLatch(1);

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch flowingLatch = new CountDownLatch(1);
    webRtcEp.addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangedEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          flowingLatch.countDown();
        }
      }
    });

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });

    // Test execution
    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.RCV_ONLY);
    playerEp.play();

    // Assertions
    Assert.assertTrue(
        "Not received FLOWING IN event in webRtcEp: " + mediaUrl + " " + webRtcChannel,
        flowingLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    Assert.assertTrue(
        "Not received media (timeout waiting playing event): " + mediaUrl + " " + webRtcChannel,
        getPage().waitForEvent("playing"));

    if (webRtcChannel == WebRtcChannel.AUDIO_ONLY
        || webRtcChannel == WebRtcChannel.AUDIO_AND_VIDEO) {
      // Checking continuity of the audio
      getPage().activatePeerConnectionInboundStats("webRtcPeer.peerConnection");

      gettingStats.schedule(new CheckAudioTimerTask(errorContinuityAudiolatch, getPage()), 100,
          200);
    }

    if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
      Assert.assertTrue("The color of the video should be " + expectedColor + ": " + mediaUrl + " "
          + webRtcChannel, getPage().similarColorAt(expectedColor, x, y));
    }
    Assert.assertTrue("Not received EOS event in player: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));
    gettingStats.cancel();

    double currentTime = getPage().getCurrentTime();
    if (playtime > 0) {
      Assert.assertTrue("Error in play time (expected: " + playtime + " sec, real: " + currentTime
          + " sec): " + mediaUrl + " " + webRtcChannel, getPage().compare(playtime, currentTime));
    }

    if (webRtcChannel == WebRtcChannel.AUDIO_ONLY
        || webRtcChannel == WebRtcChannel.AUDIO_AND_VIDEO) {
      Assert.assertTrue("Check audio. There were more than 2 seconds without receiving packets",
          errorContinuityAudiolatch.getCount() == 1);
    }

    // Release Media Pipeline
    playerEp.release();
    mp.release();
  }

  public void testPlayerPause(String mediaUrl, WebRtcChannel webRtcChannel, int pauseTimeSeconds,
      Color[] expectedColors) throws Exception {

    Timer gettingStats = new Timer();
    final CountDownLatch errorContinuityAudiolatch = new CountDownLatch(1);

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);
    playerEp.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        eosLatch.countDown();
      }
    });

    final CountDownLatch flowingLatch = new CountDownLatch(1);
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
        "Not received FLOWING IN event in webRtcEp: " + mediaUrl + " " + webRtcChannel,
        flowingLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    Assert.assertTrue(
        "Not received media (timeout waiting playing event): " + mediaUrl + " " + webRtcChannel,
        getPage().waitForEvent("playing"));

    if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
      // Assert initial color, pause stream and wait x seconds
      Assert.assertTrue("At the beginning, the color of the video should be " + expectedColors[0],
          getPage().similarColor(expectedColors[0]));
    } else {
      Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds / 2));
    }

    playerEp.pause();
    Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds));
    if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
      Assert.assertTrue("After the pause, the color of the video should be " + expectedColors[0],
          getPage().similarColor(expectedColors[0]));
    }

    playerEp.play();

    if (webRtcChannel == WebRtcChannel.AUDIO_ONLY
        || webRtcChannel == WebRtcChannel.AUDIO_AND_VIDEO) {
      // Checking continuity of the audio
      getPage().activatePeerConnectionInboundStats("webRtcPeer.peerConnection");

      gettingStats.schedule(new CheckAudioTimerTask(errorContinuityAudiolatch, getPage()), 100,
          200);
    }

    if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
      for (Color expectedColor : expectedColors) {
        Assert.assertTrue(
            "After the pause and the play, the color of the video should be " + expectedColor,
            getPage().similarColor(expectedColor));
      }
    }

    if (webRtcChannel == WebRtcChannel.AUDIO_ONLY
        || webRtcChannel == WebRtcChannel.AUDIO_AND_VIDEO) {
      Assert.assertTrue("Check audio. There were more than 2 seconds without receiving packets",
          errorContinuityAudiolatch.getCount() == 1);
    }

    // Assertions
    Assert.assertTrue("Not received EOS event in player: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    gettingStats.cancel();

    // Release Media Pipeline
    playerEp.release();
    mp.release();
  }

  public void testPlayerSeek(String mediaUrl, WebRtcChannel webRtcChannel, int pauseTimeSeconds,
      Map<Integer, Color> expectedPositionAndColor) throws Exception {

    Timer gettingStats = new Timer();
    final CountDownLatch errorContinuityAudiolatch = new CountDownLatch(1);

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

    if (webRtcChannel == WebRtcChannel.AUDIO_ONLY
        || webRtcChannel == WebRtcChannel.AUDIO_AND_VIDEO) {
      // Checking continuity of the audio
      getPage().activatePeerConnectionInboundStats("webRtcPeer.peerConnection");

      gettingStats.schedule(new CheckAudioTimerTask(errorContinuityAudiolatch, getPage()), 100,
          200);
    }

    Assert.assertTrue(
        "Not received FLOWING IN event in webRtcEp: " + mediaUrl + " " + webRtcChannel,
        flowingLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // TODO: Check with playerEp.getVideoInfo().getIsSeekable() if the video is seekable. If not,
    // assert with exception from KMS

    // Assertions

    Thread.sleep(TimeUnit.SECONDS.toMillis(pauseTimeSeconds));
    for (Integer position : expectedPositionAndColor.keySet()) {
      log.debug("Try to set position in {}", position);
      playerEp.setPosition(position);
      if (webRtcChannel != WebRtcChannel.AUDIO_ONLY) {
        Assert.assertTrue(
            "After set position to " + position + "ms, the color of the video should be "
                + expectedPositionAndColor.get(position),
            getPage().similarColor(expectedPositionAndColor.get(position)));
      }
    }

    if (webRtcChannel == WebRtcChannel.AUDIO_ONLY
        || webRtcChannel == WebRtcChannel.AUDIO_AND_VIDEO) {
      Assert.assertTrue("Check audio. There were more than 2 seconds without receiving packets",
          errorContinuityAudiolatch.getCount() == 1);
    }

    Assert.assertTrue("Not received EOS event in player: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    gettingStats.cancel();

    // Release Media Pipeline
    playerEp.release();
    mp.release();
  }

}
