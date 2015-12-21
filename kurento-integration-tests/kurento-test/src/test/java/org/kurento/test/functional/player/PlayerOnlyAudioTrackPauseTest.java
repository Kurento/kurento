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

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.TestScenario;

/**
 * Test of a the pause feature with only track audio for a PlayerEndpoint. <br>
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
public class PlayerOnlyAudioTrackPauseTest extends SimplePlayer {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(String mediaUrl) throws Exception {
    int pauseTimeSeconds = 10;
    testPlayerPause(mediaUrl, WebRtcChannel.AUDIO_ONLY, pauseTimeSeconds, null);
  }

  @Test
  public void testPlayerPauseMp3() throws Exception {
    // Test data
    String mediaUrl = "http://files.kurento.org/audio/10sec/cinema.mp3";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseM4a() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/audio/10sec/cinema.m4a";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseOgg() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/audio/10sec/cinema.ogg";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseWav() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/audio/10sec/cinema.wav";
    initTest(mediaUrl);
  }

  @Test
  public void testPlayerPauseWma() throws Exception {
    // Test data
    final String mediaUrl = "http://files.kurento.org/audio/10sec/cinema.wma";
    initTest(mediaUrl);
  }

}
