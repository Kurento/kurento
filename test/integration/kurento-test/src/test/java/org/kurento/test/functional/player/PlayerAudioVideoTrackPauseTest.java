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
 * Test of a the pause feature for a PlayerEndpoint.
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
public class PlayerAudioVideoTrackPauseTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    int pauseTimeSeconds = 10;
    final Color[] expectedColors = { Color.RED, Color.GREEN, Color.BLUE };

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayerPause(mediaUrl, WebRtcChannel.AUDIO_AND_VIDEO, pauseTimeSeconds, expectedColors);
  }

  @Test
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

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS3Ogv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.ogv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS3Mkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mkv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS3Avi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.avi";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS3Webm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.webm";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS3Mov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mov";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS33gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.3gp";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseS3Mp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mp4";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepositoryOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.ogv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepositoryMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mkv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepositoryAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.avi";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepositoryWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.webm";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepositoryMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mov";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepository3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.3gp";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerAudioVideoPauseRepositoryMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/15sec/rgb.mp4";
    initTest(MONGODB, mediaUrl);
  }
}
