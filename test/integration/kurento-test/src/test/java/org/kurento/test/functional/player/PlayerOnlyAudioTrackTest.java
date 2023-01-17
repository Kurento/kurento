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

import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.MONGODB;
import static org.kurento.test.config.Protocol.S3;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a PlayerEndpoint with different types of media sources (MP3, WAV ... all with ONLY AUDIO)
 * connected to a WebRtc Endpoint
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
 * <li>(KMS) PlayerEndpoint reads media source (from HTTP, FILE and S3) and connects to a
 * WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time in remote video should be as expected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class PlayerOnlyAudioTrackTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  private void initTest(Protocol protocol, String nameMedia) throws Exception {
    String mediaUrl = getMediaUrl(protocol, nameMedia);

    testPlayer(mediaUrl, AUDIO_ONLY, 10);
  }

  @Test
  public void testPlayerOnlyAudioTrackHttpMp3() throws Exception {
    initTest(HTTP, "/audio/10sec/birds.mp3");
  }

  @Test
  public void testPlayerOnlyAudioTrackHttpWav() throws Exception {
    initTest(HTTP, "/audio/10sec/counter.wav");
  }

  @Test
  public void testPlayerOnlyAudioTrackHttpFlac() throws Exception {
    initTest(HTTP, "/audio/10sec/cinema.flac");
  }

  @Test
  public void testPlayerOnlyAudioTrackHttpOgg() throws Exception {
    initTest(HTTP, "/audio/10sec/fiware.ogg");
  }

  @Test
  public void testPlayerOnlyAudioTrackHttpM4a() throws Exception {
    initTest(HTTP, "/audio/10sec/left-right.m4a");
  }

  @Test
  public void testPlayerOnlyAudioTrackHttpWma() throws Exception {
    initTest(HTTP, "/audio/10sec/meet.wma");
  }

  @Test
  public void testPlayerOnlyAudioTrackFileMp3() throws Exception {
    initTest(FILE, "/audio/10sec/birds.mp3");
  }

  @Test
  public void testPlayerOnlyAudioTrackFileWav() throws Exception {
    initTest(FILE, "/audio/10sec/counter.wav");
  }

  @Test
  public void testPlayerOnlyAudioTrackFileFlac() throws Exception {
    initTest(FILE, "/audio/10sec/cinema.flac");
  }

  @Test
  public void testPlayerOnlyAudioTrackFileOgg() throws Exception {
    initTest(FILE, "/audio/10sec/fiware.ogg");
  }

  @Test
  public void testPlayerOnlyAudioTrackFileM4a() throws Exception {
    initTest(FILE, "/audio/10sec/left-right.m4a");
  }

  @Test
  public void testPlayerOnlyAudioTrackFileWma() throws Exception {
    initTest(FILE, "/audio/10sec/meet.wma");
  }

  @Ignore
  public void testPlayerOnlyAudioTrackS3Mp3() throws Exception {
    initTest(S3, "/audio/10sec/birds.mp3");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackS3Wav() throws Exception {
    initTest(S3, "/audio/10sec/counter.wav");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackS3Flac() throws Exception {
    initTest(S3, "/audio/10sec/cinema.flac");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackS3Ogg() throws Exception {
    initTest(S3, "/audio/10sec/fiware.ogg");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackS3M4a() throws Exception {
    initTest(S3, "/audio/10sec/left-right.m4a");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackS3Wma() throws Exception {
    initTest(S3, "/audio/10sec/meet.wma");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackRepositoryMp3() throws Exception {
    initTest(MONGODB, "/audio/10sec/birds.mp3");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackRepositoryWav() throws Exception {
    initTest(MONGODB, "/audio/10sec/counter.wav");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackRepositoryFlac() throws Exception {
    initTest(MONGODB, "/audio/10sec/cinema.flac");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackRepositoryOgg() throws Exception {
    initTest(MONGODB, "/audio/10sec/fiware.ogg");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackRepositoryM4a() throws Exception {
    initTest(MONGODB, "/audio/10sec/left-right.m4a");
  }

  @Ignore
  @Test
  public void testPlayerOnlyAudioTrackRepositoryWma() throws Exception {
    initTest(MONGODB, "/audio/10sec/meet.wma");
  }
}
