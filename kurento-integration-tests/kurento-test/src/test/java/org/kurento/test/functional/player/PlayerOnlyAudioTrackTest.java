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

import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.S3;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;

/**
 * Test of a PlayerEndpoint with different types of media sources (MP3, WAV ... all with ONLY AUDIO)
 * connected to a WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) PlayerEndpoint reads media source (from HTTP, FILE and S3) and connects to a
 * WebRtcEndpoint <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · EOS event should arrive to player <br>
 * · Play time in remote video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
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

  @Ignore
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
  public void testPlayerOnlyAudioTrackS3Wav() throws Exception {
    initTest(S3, "/audio/10sec/counter.wav");
  }

  @Ignore
  public void testPlayerOnlyAudioTrackS3Flac() throws Exception {
    initTest(S3, "/audio/10sec/cinema.flac");
  }

  @Test
  public void testPlayerOnlyAudioTrackS3Ogg() throws Exception {
    initTest(S3, "/audio/10sec/fiware.ogg");
  }

  @Ignore
  public void testPlayerOnlyAudioTrackS3M4a() throws Exception {
    initTest(S3, "/audio/10sec/left-right.m4a");
  }

  @Test
  public void testPlayerOnlyAudioTrackS3Wma() throws Exception {
    initTest(S3, "/audio/10sec/meet.wma");
  }

}
