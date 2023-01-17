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

package org.kurento.test.stability.player;

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
import org.kurento.test.base.StabilityTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of multiple seek feature in a PlayerEndpoint. <br>
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
 * PlayerEndpoint is sought three times and then repeat numSeeks times <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Color or the video should remain when a video is sought <br>
 * · After the seek, the color or the video should change <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.1.1
 */
public class PlayerMultipleAudioVideoTrackSeekTest extends StabilityTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    int pauseTimeSeconds = 3;
    int numSeeks = getTestSeekRepetitions();
    final Map<Integer, Color> expectedPositionAndColor = new LinkedHashMap<Integer, Color>();
    expectedPositionAndColor.put(2000, Color.RED);
    expectedPositionAndColor.put(20000, Color.BLUE);
    expectedPositionAndColor.put(10000, Color.GREEN);

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayerMultipleSeek(mediaUrl, WebRtcChannel.AUDIO_AND_VIDEO, pauseTimeSeconds, numSeeks,
        expectedPositionAndColor);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttpOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.ogv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttpMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mkv";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttpAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.avi";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttpWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.webm";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttpMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mov";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttp3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.3gp";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekHttpMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mp4";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFileOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.ogv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFileMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mkv";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFileAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.avi";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFileWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.webm";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFileMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mov";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFile3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.3gp";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleAudioVideoSeekFileMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mp4";
    initTest(FILE, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS3Ogv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.ogv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS3Mkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mkv";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS3Avi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.avi";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS3Webm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.webm";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS3Mov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mov";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS33gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.3gp";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekS3Mp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mp4";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepositoryOgv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.ogv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepositoryMkv() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mkv";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepositoryAvi() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.avi";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepositoryWebm() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.webm";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepositoryMov() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mov";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepository3gp() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.3gp";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleAudioVideoSeekRepositoryMp4() throws Exception {
    // Test data
    final String mediaUrl = "/video/30sec/rgb.mp4";
    initTest(MONGODB, mediaUrl);
  }
}
