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

import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.S3;

import java.awt.Color;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
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
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.1.1
 */
public class PlayerAudioVideoTrackPauseTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    int pauseTimeSeconds = 3;
    final Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayerPause(mediaUrl, WebRtcChannel.AUDIO_AND_VIDEO, pauseTimeSeconds, expectedColors);
  }

  @Ignore
  public void testPlayerAudioVideoPauseHttpOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.ogv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseHttpMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mkv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseHttpAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.avi";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseHttpWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.webm";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseHttpMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mov";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseHttp3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.3gp";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseHttpMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mp4";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFileOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.ogv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFileMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mkv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFileAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.avi";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFileWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.webm";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFileMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mov";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFile3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.3gp";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseFileMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mp4";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS3Ogv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.ogv";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS3Mkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mkv";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS3Avi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.avi";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS3Webm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.webm";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS3Mov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mov";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS33gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.3gp";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerAudioVideoPauseS3Mp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mp4";
    initTest(S3, mediaUrl);
  }
}
