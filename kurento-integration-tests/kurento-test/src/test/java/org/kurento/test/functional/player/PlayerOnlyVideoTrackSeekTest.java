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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a the seek feature for a Player Endpoint
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
 * PlayerEndpoint is sought three times and then resumed</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color or the video should remain when a video is sought</li>
 * <li>After the seek, the color or the video should change</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.1.1
 */
public class PlayerOnlyVideoTrackSeekTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {

    final Map<Integer, Color> expectedPositionAndColor = new LinkedHashMap<Integer, Color>();
    expectedPositionAndColor.put(2000, Color.RED);
    expectedPositionAndColor.put(20000, Color.BLUE);
    expectedPositionAndColor.put(10000, Color.GREEN);
    expectedPositionAndColor.put(2000, Color.RED);
    expectedPositionAndColor.put(20100, Color.BLUE);

    String mediaUrl = getMediaUrl(protocol, nameMedia);
    int pauseTimeSeconds = 3;
    testPlayerSeek(mediaUrl, WebRtcChannel.VIDEO_ONLY, pauseTimeSeconds, expectedPositionAndColor);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttpOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.ogv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttpMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mkv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttpAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.avi";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttpWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.webm";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttpMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mov";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttp3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.3gp";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekHttpMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mp4";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFileOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.ogv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFileMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mkv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFileAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.avi";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFileWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.webm";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFileMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mov";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFile3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.3gp";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyVideoSeekFileMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mp4";
    initTest(FILE, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS3Ogv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.ogv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS3Mkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mkv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS3Avi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.avi";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS3Webm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.webm";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS3Mov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mov";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS33gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.3gp";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoSeekS3Mp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mp4";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepositoryOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.ogv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepositoryMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mkv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepositoryAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.avi";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepositoryWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.webm";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepositoryMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mov";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepository3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.3gp";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyVideoRepositoryMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgbOnlyVideo.mp4";
    initTest(MONGODB, mediaUrl);
  }
}
