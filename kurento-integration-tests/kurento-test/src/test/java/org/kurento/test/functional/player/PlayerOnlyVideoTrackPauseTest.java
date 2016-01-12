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

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a the pause feature with only track video for a PlayerEndpoint. <br>
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
public class PlayerOnlyVideoTrackPauseTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    int pauseTimeSeconds = 3;
    final Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayerPause(mediaUrl, WebRtcChannel.VIDEO_ONLY, pauseTimeSeconds, expectedColors);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttpOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.ogv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttpMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mkv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttpAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.avi";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttpWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.webm";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttpMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mov";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttp3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.3gp";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseHttpMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mp4";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFileOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.ogv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFileMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mkv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFileAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.avi";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFileWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.webm";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFileMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mov";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFile3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.3gp";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseFileMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mp4";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS3Ogv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.ogv";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS3Mkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mkv";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS3Avi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.avi";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS3Webm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.webm";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS3Mov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mov";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS33gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.3gp";
    initTest(S3, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoPauseS3Mp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mp4";
    initTest(S3, mediaUrl);
  }
}
