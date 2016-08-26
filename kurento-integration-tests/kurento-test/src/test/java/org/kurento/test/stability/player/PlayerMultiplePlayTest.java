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

import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
import static org.kurento.test.browser.WebRtcChannel.VIDEO_ONLY;
import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.VideoFormat.AVI;
import static org.kurento.test.config.VideoFormat.MKV;
import static org.kurento.test.config.VideoFormat.MOV;
import static org.kurento.test.config.VideoFormat.MP4;
import static org.kurento.test.config.VideoFormat.OGV;
import static org.kurento.test.config.VideoFormat.THIRDGP;
import static org.kurento.test.config.VideoFormat.WEBM;

import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.commons.testing.SystemStabilityTests;
import org.kurento.test.config.TestScenario;
import org.kurento.test.functional.player.SimplePlayer;

/**
 * Test of stability for a PlayerEndpoint (play many times different videos).
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
 * <li>(KMS) PlayerEndpoint reads different media sources (HTTP/FILE) and different format (WEBM,
 * OGV, MOV, MP4, MKV, AVI, 3GP) and connects to a WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media. WebRtcPeer can be configured to receive both
 * video and audio, only video, or only audio</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>The color of the received video should be as expected</li><
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
@Category(SystemStabilityTests.class)
public class PlayerMultiplePlayTest extends SimplePlayer {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttp3gp() throws Exception {
    testPlayerWithSmallFile(HTTP, THIRDGP, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttp3gp() throws Exception {
    testPlayerWithSmallFile(HTTP, THIRDGP, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttp3gp() throws Exception {
    testPlayerWithSmallFile(HTTP, THIRDGP, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttpAvi() throws Exception {
    testPlayerWithSmallFile(HTTP, AVI, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttpAvi() throws Exception {
    testPlayerWithSmallFile(HTTP, AVI, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttpAvi() throws Exception {
    testPlayerWithSmallFile(HTTP, AVI, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttpMkv() throws Exception {
    testPlayerWithSmallFile(HTTP, MKV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttpMkv() throws Exception {
    testPlayerWithSmallFile(HTTP, MKV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttpMkv() throws Exception {
    testPlayerWithSmallFile(HTTP, MKV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttpMov() throws Exception {
    testPlayerWithSmallFile(HTTP, MOV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttpMov() throws Exception {
    testPlayerWithSmallFile(HTTP, MOV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttpMov() throws Exception {
    testPlayerWithSmallFile(HTTP, MOV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttpMp4() throws Exception {
    testPlayerWithSmallFile(HTTP, MP4, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttpMp4() throws Exception {
    testPlayerWithSmallFile(HTTP, MP4, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttpMp4() throws Exception {
    testPlayerWithSmallFile(HTTP, MP4, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttpOgv() throws Exception {
    testPlayerWithSmallFile(HTTP, OGV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttpOgv() throws Exception {
    testPlayerWithSmallFile(HTTP, OGV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttpOgv() throws Exception {
    testPlayerWithSmallFile(HTTP, OGV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoHttpWebm() throws Exception {
    testPlayerWithSmallFile(HTTP, WEBM, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyHttpWebm() throws Exception {
    testPlayerWithSmallFile(HTTP, WEBM, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyHttpWebm() throws Exception {
    testPlayerWithSmallFile(HTTP, WEBM, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFile3gp() throws Exception {
    testPlayerWithSmallFile(FILE, THIRDGP, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFile3gp() throws Exception {
    testPlayerWithSmallFile(FILE, THIRDGP, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFile3gp() throws Exception {
    testPlayerWithSmallFile(FILE, THIRDGP, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFileAvi() throws Exception {
    testPlayerWithSmallFile(FILE, AVI, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFileAvi() throws Exception {
    testPlayerWithSmallFile(FILE, AVI, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFileAvi() throws Exception {
    testPlayerWithSmallFile(FILE, AVI, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFileMkv() throws Exception {
    testPlayerWithSmallFile(FILE, MKV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFileMkv() throws Exception {
    testPlayerWithSmallFile(FILE, MKV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFileMkv() throws Exception {
    testPlayerWithSmallFile(FILE, MKV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFileMov() throws Exception {
    testPlayerWithSmallFile(FILE, MOV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFileMov() throws Exception {
    testPlayerWithSmallFile(FILE, MOV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFileMov() throws Exception {
    testPlayerWithSmallFile(FILE, MOV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFileMp4() throws Exception {
    testPlayerWithSmallFile(FILE, MP4, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFileMp4() throws Exception {
    testPlayerWithSmallFile(FILE, MP4, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFileMp4() throws Exception {
    testPlayerWithSmallFile(FILE, MP4, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFileOgv() throws Exception {
    testPlayerWithSmallFile(FILE, OGV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFileOgv() throws Exception {
    testPlayerWithSmallFile(FILE, OGV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFileOgv() throws Exception {
    testPlayerWithSmallFile(FILE, OGV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayAudioVideoFileWebm() throws Exception {
    testPlayerWithSmallFile(FILE, WEBM, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerMultiplePlayAudioOnlyFileWebm() throws Exception {
    testPlayerWithSmallFile(FILE, WEBM, AUDIO_ONLY);
  }

  @Test
  public void testPlayerMultiplePlayVideoOnlyFileWebm() throws Exception {
    testPlayerWithSmallFile(FILE, WEBM, VIDEO_ONLY);
  }
}
