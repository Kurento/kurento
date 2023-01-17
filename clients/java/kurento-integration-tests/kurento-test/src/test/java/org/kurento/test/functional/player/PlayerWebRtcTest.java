/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
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
 * 3GP ... all with video and audio) connected to a WebRtc Endpoint
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
 * <li>(Browser) WebRtcPeer in rcv-only receives media. WebRtcPeer can be configured to receive both
 * video and audio, only video, or only audio</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>The color of the received video should be as expected</li>
 * <li>EOS event should arrive to player</li>
 * <li>Play time in remote video should be as expected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class PlayerWebRtcTest extends FunctionalPlayerTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttp3gp() throws Exception {
    testPlayerWithSmallFile(HTTP, THIRDGP, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttp3gp() throws Exception {
    testPlayerWithSmallFile(HTTP, THIRDGP, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttp3gp() throws Exception {
    testPlayerWithSmallFile(HTTP, THIRDGP, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttpAvi() throws Exception {
    testPlayerWithSmallFile(HTTP, AVI, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpAvi() throws Exception {
    testPlayerWithSmallFile(HTTP, AVI, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttpAvi() throws Exception {
    testPlayerWithSmallFile(HTTP, AVI, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttpMkv() throws Exception {
    testPlayerWithSmallFile(HTTP, MKV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpMkv() throws Exception {
    testPlayerWithSmallFile(HTTP, MKV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttpMkv() throws Exception {
    testPlayerWithSmallFile(HTTP, MKV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttpMov() throws Exception {
    testPlayerWithSmallFile(HTTP, MOV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpMov() throws Exception {
    testPlayerWithSmallFile(HTTP, MOV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttpMov() throws Exception {
    testPlayerWithSmallFile(HTTP, MOV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttpMp4() throws Exception {
    testPlayerWithSmallFile(HTTP, MP4, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpMp4() throws Exception {
    testPlayerWithSmallFile(HTTP, MP4, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttpMp4() throws Exception {
    testPlayerWithSmallFile(HTTP, MP4, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttpOgv() throws Exception {
    testPlayerWithSmallFile(HTTP, OGV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpOgv() throws Exception {
    testPlayerWithSmallFile(HTTP, OGV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttpOgv() throws Exception {
    testPlayerWithSmallFile(HTTP, OGV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoHttpWebm() throws Exception {
    testPlayerWithSmallFile(HTTP, WEBM, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyHttpWebm() throws Exception {
    testPlayerWithSmallFile(HTTP, WEBM, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyHttpWebm() throws Exception {
    testPlayerWithSmallFile(HTTP, WEBM, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFile3gp() throws Exception {
    testPlayerWithSmallFile(FILE, THIRDGP, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFile3gp() throws Exception {
    testPlayerWithSmallFile(FILE, THIRDGP, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFile3gp() throws Exception {
    testPlayerWithSmallFile(FILE, THIRDGP, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFileAvi() throws Exception {
    testPlayerWithSmallFile(FILE, AVI, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileAvi() throws Exception {
    testPlayerWithSmallFile(FILE, AVI, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFileAvi() throws Exception {
    testPlayerWithSmallFile(FILE, AVI, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFileMkv() throws Exception {
    testPlayerWithSmallFile(FILE, MKV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileMkv() throws Exception {
    testPlayerWithSmallFile(FILE, MKV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFileMkv() throws Exception {
    testPlayerWithSmallFile(FILE, MKV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFileMov() throws Exception {
    testPlayerWithSmallFile(FILE, MOV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileMov() throws Exception {
    testPlayerWithSmallFile(FILE, MOV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFileMov() throws Exception {
    testPlayerWithSmallFile(FILE, MOV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFileMp4() throws Exception {
    testPlayerWithSmallFile(FILE, MP4, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileMp4() throws Exception {
    testPlayerWithSmallFile(FILE, MP4, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFileMp4() throws Exception {
    testPlayerWithSmallFile(FILE, MP4, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFileOgv() throws Exception {
    testPlayerWithSmallFile(FILE, OGV, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileOgv() throws Exception {
    testPlayerWithSmallFile(FILE, OGV, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFileOgv() throws Exception {
    testPlayerWithSmallFile(FILE, OGV, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoFileWebm() throws Exception {
    testPlayerWithSmallFile(FILE, WEBM, AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyFileWebm() throws Exception {
    testPlayerWithSmallFile(FILE, WEBM, VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyFileWebm() throws Exception {
    testPlayerWithSmallFile(FILE, WEBM, AUDIO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioAndVideoRtsp() throws Exception {
    testPlayerWithRtsp(AUDIO_AND_VIDEO);
  }

  @Test
  public void testPlayerWebRtcVideoOnlyRtsp() throws Exception {
    testPlayerWithRtsp(VIDEO_ONLY);
  }

  @Test
  public void testPlayerWebRtcAudioOnlyRtsp() throws Exception {
    testPlayerWithRtsp(AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS33gp() throws Exception {
    testPlayerWithSmallFile(S3, THIRDGP, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS33gp() throws Exception {
    testPlayerWithSmallFile(S3, THIRDGP, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS33gp() throws Exception {
    testPlayerWithSmallFile(S3, THIRDGP, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS3Avi() throws Exception {
    testPlayerWithSmallFile(S3, AVI, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Avi() throws Exception {
    testPlayerWithSmallFile(S3, AVI, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS3Avi() throws Exception {
    testPlayerWithSmallFile(S3, AVI, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS3Mkv() throws Exception {
    testPlayerWithSmallFile(S3, MKV, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Mkv() throws Exception {
    testPlayerWithSmallFile(S3, MKV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS3Mkv() throws Exception {
    testPlayerWithSmallFile(S3, MKV, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS3Mov() throws Exception {
    testPlayerWithSmallFile(S3, MOV, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Mov() throws Exception {
    testPlayerWithSmallFile(S3, MOV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS3Mov() throws Exception {
    testPlayerWithSmallFile(S3, MOV, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS3Mp4() throws Exception {
    testPlayerWithSmallFile(S3, MP4, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Mp4() throws Exception {
    testPlayerWithSmallFile(S3, MP4, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS3Mp4() throws Exception {
    testPlayerWithSmallFile(S3, MP4, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS3Ogv() throws Exception {
    testPlayerWithSmallFile(S3, OGV, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Ogv() throws Exception {
    testPlayerWithSmallFile(S3, OGV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS3Ogv() throws Exception {
    testPlayerWithSmallFile(S3, OGV, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoS3Webm() throws Exception {
    testPlayerWithSmallFile(S3, WEBM, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyS3Webm() throws Exception {
    testPlayerWithSmallFile(S3, WEBM, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyS3Webm() throws Exception {
    testPlayerWithSmallFile(S3, WEBM, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepository3gp() throws Exception {
    testPlayerWithSmallFile(MONGODB, THIRDGP, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepository3gp() throws Exception {
    testPlayerWithSmallFile(MONGODB, THIRDGP, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepository3gp() throws Exception {
    testPlayerWithSmallFile(MONGODB, THIRDGP, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepositoryAvi() throws Exception {
    testPlayerWithSmallFile(MONGODB, AVI, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryAvi() throws Exception {
    testPlayerWithSmallFile(MONGODB, AVI, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepositoryAvi() throws Exception {
    testPlayerWithSmallFile(MONGODB, AVI, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepositoryMkv() throws Exception {
    testPlayerWithSmallFile(MONGODB, MKV, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryMkv() throws Exception {
    testPlayerWithSmallFile(MONGODB, MKV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepositoryMkv() throws Exception {
    testPlayerWithSmallFile(MONGODB, MKV, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepositoryMov() throws Exception {
    testPlayerWithSmallFile(MONGODB, MOV, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryMov() throws Exception {
    testPlayerWithSmallFile(MONGODB, MOV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepositoryMov() throws Exception {
    testPlayerWithSmallFile(MONGODB, MOV, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepositoryMp4() throws Exception {
    testPlayerWithSmallFile(MONGODB, MP4, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryMp4() throws Exception {
    testPlayerWithSmallFile(MONGODB, MP4, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepositoryMp4() throws Exception {
    testPlayerWithSmallFile(MONGODB, MP4, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepositoryOgv() throws Exception {
    testPlayerWithSmallFile(MONGODB, OGV, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryOgv() throws Exception {
    testPlayerWithSmallFile(MONGODB, OGV, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepositoryOgv() throws Exception {
    testPlayerWithSmallFile(MONGODB, OGV, AUDIO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioAndVideoRepositoryWebm() throws Exception {
    testPlayerWithSmallFile(MONGODB, WEBM, AUDIO_AND_VIDEO);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcVideoOnlyRepositoryWebm() throws Exception {
    testPlayerWithSmallFile(MONGODB, WEBM, VIDEO_ONLY);
  }

  @Ignore
  @Test
  public void testPlayerWebRtcAudioOnlyRepositoryWebm() throws Exception {
    testPlayerWithSmallFile(MONGODB, WEBM, AUDIO_ONLY);
  }
}
