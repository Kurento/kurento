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
package org.kurento.test.functional.player;

import java.awt.Color;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.TestScenario;

/**
 * Test of a the pause feature for a PlayerEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) During the playback of a stream from a PlayerEndpoint to a WebRtcEndpoint, the
 * PlayerEndpoint is paused and then resumed <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Color or the video should remain when a video is paused <br>
 * · After the pause, the color or the video should change <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class PlayerAudioVideoTrackPauseTest extends SimplePlayer {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(String mediaUrl) throws Exception {
    int pauseTimeSeconds = 10;
    final Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };
    testPlayerPause(mediaUrl, WebRtcChannel.AUDIO_AND_VIDEO, pauseTimeSeconds, expectedColors);
  }

  @Test
  public void testPlayerPauseOgv() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.ogv";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseMkv() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.mkv";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseAvi() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.avi";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseWebm() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.webm";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseMov() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.mov";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPause3gp() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.3gp";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseMp4() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/video/15sec/rgb.mp4";
    initTest(mediaUrl);
  }
}
