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

import static org.kurento.test.browser.WebRtcChannel.VIDEO_ONLY;
import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.MONGODB;
import static org.kurento.test.config.Protocol.S3;
import static org.kurento.test.config.VideoFormat.AVI;
import static org.kurento.test.config.VideoFormat.MKV;
import static org.kurento.test.config.VideoFormat.MOV;
import static org.kurento.test.config.VideoFormat.MP4;
import static org.kurento.test.config.VideoFormat.OGV;
import static org.kurento.test.config.VideoFormat.THIRDGP;
import static org.kurento.test.config.VideoFormat.WEBM;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.config.TestScenario;

/**
 * Test of a PlayerEndpoint with different types of media sources (WEBM, OGV, MOV, MP4, MKV, AVI,
 * 3GP ... all with ONLY VIDEO) connected to a WebRtc Endpoint
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
 *
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>The color of the received video should be as expected</li>
 * </ul>
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
public class PlayerOnlyVideoTrackTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttp3gp() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, THIRDGP, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpAvi() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, AVI, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpMkv() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, MKV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpMov() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, MOV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpMp4() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, MP4, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpOgv() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, OGV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpWebm() throws Exception {
    testPlayerWithSmallFileVideoOnly(HTTP, WEBM, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFile3gp() throws Exception {
    testPlayerWithSmallFileVideoOnly(FILE, THIRDGP, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileAvi() throws Exception {
    testPlayerWithSmallFileVideoOnly(FILE, AVI, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileMkv() throws Exception {
    testPlayerWithSmallFileVideoOnly(FILE, MKV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileMov() throws Exception {
    testPlayerWithSmallFileVideoOnly(FILE, MOV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileMp4() throws Exception {
    testPlayerWithSmallFileVideoOnly(FILE, MP4, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileOgv() throws Exception {
    testPlayerWithSmallFileVideoOnly(FILE, OGV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS33gp() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, THIRDGP, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Avi() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, AVI, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Mkv() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, MKV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Mov() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, MOV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Mp4() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, MP4, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Ogv() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, OGV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Webm() throws Exception {
    testPlayerWithSmallFileVideoOnly(S3, WEBM, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepository3gp() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, THIRDGP, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryAvi() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, AVI, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryMkv() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, MKV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryMov() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, MOV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryMp4() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, MP4, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryOgv() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, OGV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryWebm() throws Exception {
    testPlayerWithSmallFileVideoOnly(MONGODB, WEBM, VIDEO_ONLY);
  }

}
