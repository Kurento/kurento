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

package org.kurento.test.functional.player;

import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.MONGODB;
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
 * Test of a the pause feature with only track video for a Player Endpoint
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) During the playback of a stream from a PlayerEndpoint to a WebRtcEndpoint, the
 * PlayerEndpoint is paused and then resumed</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color or the video should remain when a video is paused</li>
 * <li>After the pause, the color or the video should change</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
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
    int pauseTimeSeconds = 10;
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

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS3Ogv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.ogv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS3Mkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mkv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS3Avi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.avi";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS3Webm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.webm";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS3Mov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mov";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS33gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.3gp";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseS3Mp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mp4";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepositoryOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.ogv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepositoryMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mkv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepositoryAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.avi";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepositoryWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.webm";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepositoryMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mov";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepository3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.3gp";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoPauseRepositoryMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgbOnlyVideo.mp4";
    initTest(MONGODB, mediaUrl);
  }
}
