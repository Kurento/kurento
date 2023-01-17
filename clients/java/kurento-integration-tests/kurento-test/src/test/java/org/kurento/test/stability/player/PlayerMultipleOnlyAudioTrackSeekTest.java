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
 * Test of multiple seek feature for a PlayerEndpoint. <br>
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
 * · After the seek, the audio has continue <br>
 *
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.1.1
 */
public class PlayerMultipleOnlyAudioTrackSeekTest extends StabilityTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    int pauseTimeSeconds = 3;
    int numSeeks = getTestSeekRepetitions();
    final Map<Integer, Color> expectedPositionAndWithoutColor = new LinkedHashMap<Integer, Color>();
    expectedPositionAndWithoutColor.put(2000, null);
    expectedPositionAndWithoutColor.put(5000, null);
    expectedPositionAndWithoutColor.put(1000, null);

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayerMultipleSeek(mediaUrl, WebRtcChannel.AUDIO_ONLY, pauseTimeSeconds, numSeeks,
        expectedPositionAndWithoutColor);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekHttpMp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekHttpM4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekHttpOgg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekHttpWav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekHttpWma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekFileMp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekFileM4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekFileOgg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekFileWav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerMultipleOnlyAudioSeekFileWma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(FILE, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekS3Mp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekS3M4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekS3Ogg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekS3Wav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekS3Wma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekRepositoryMp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekRepositoryM4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekRepositoryOgg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekRepositoryWav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerMultipleOnlyAudioSeekRepositoryWma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(MONGODB, mediaUrl);
  }
}
