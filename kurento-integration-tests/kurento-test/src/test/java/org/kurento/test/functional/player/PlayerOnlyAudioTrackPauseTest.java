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

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a the pause feature with only track audio for a PlayerEndpoint.
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
public class PlayerOnlyAudioTrackPauseTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    int pauseTimeSeconds = 10;

    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayerPause(mediaUrl, WebRtcChannel.AUDIO_ONLY, pauseTimeSeconds, null);
  }

  @Test
  public void testPlayerOnlyAudioPauseHttpMp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseHttpM4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseHttpOgg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseHttpWav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseHttpWma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(HTTP, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseFileMp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseFileM4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseFileOgg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseFileWav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(FILE, mediaUrl);
  }

  @Test
  public void testPlayerOnlyAudioPauseFileWma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(FILE, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseS3Mp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseS3M4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseS3Ogg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseS3Wav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseS3Wma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(S3, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseRepositoryMp3() throws Exception {
    // Test data
    String mediaUrl = "/audio/10sec/cinema.mp3";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseRepositoryM4a() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.m4a";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseRepositoryOgg() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.ogg";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseRepositoryWav() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wav";
    initTest(MONGODB, mediaUrl);
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioPauseRepositoryWma() throws Exception {
    // Test data
    final String mediaUrl = "/audio/10sec/cinema.wma";
    initTest(MONGODB, mediaUrl);
  }
}
