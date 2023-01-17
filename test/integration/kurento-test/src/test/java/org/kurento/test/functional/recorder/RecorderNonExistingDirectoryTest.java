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

package org.kurento.test.functional.recorder;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kurento.client.MediaProfileSpecType.WEBM;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.test.mediainfo.AssertMedia;

/**
 * Test of a recorder, using the stream source from a WebRtcEndpoint. Tests recording with audio and
 * video. The path for recording won't exist and the KMS should create it
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>WebRtcEndpoint -> WebRtcEndpoint & RecorderEndpoint</li></li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Two media pipelines. First WebRtcEndpoint to RecorderEndpoint (recording) and then
 * PlayerEndpoint -> WebRtcEndpoint (play of the recording).</li>
 * <li>(Browser) First a WebRtcPeer in send-only sends media. Second, other WebRtcPeer in rcv-only
 * receives media</li>
 * </ul>
 * Main assertion(s):
 * <ul>
 * <li>Codecs should be as expected (in the recording)</li>
 * </ul>
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.4.1
 */
public class RecorderNonExistingDirectoryTest extends BaseRecorder {

  private static final int PLAYTIME = 20; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testRecorderNonExistingDirectoryWebm() throws Exception {
    doTest(WEBM, EXPECTED_VIDEO_CODEC_WEBM, EXPECTED_AUDIO_CODEC_WEBM, EXTENSION_WEBM);
  }

  public void doTest(MediaProfileSpecType mediaProfileSpecType, String expectedVideoCodec,
      String expectedAudioCodec, String extension) throws Exception {

    final CountDownLatch recorderLatch = new CountDownLatch(1);

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();

    String recordingFile = getRecordUrl(extension);

    recordingFile = recordingFile.replace(getSimpleTestName(),
        new Date().getTime() + File.separator + getSimpleTestName());

    log.debug("The path non existing is {} ", recordingFile);

    RecorderEndpoint recorderEp = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(mediaProfileSpecType).build();
    webRtcEp.connect(webRtcEp);
    webRtcEp.connect(recorderEp);

    getPage().subscribeEvents("playing");
    getPage().initWebRtc(webRtcEp, AUDIO_AND_VIDEO, WebRtcMode.SEND_RCV);
    recorderEp.record();

    // Wait until event playing in the remote stream
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage().waitForEvent("playing"));

    Thread.sleep(SECONDS.toMillis(PLAYTIME));

    recorderEp.stopAndWait(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        recorderLatch.countDown();
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        recorderLatch.countDown();
      }
    });

    Assert.assertTrue("Not stop properly",
        recorderLatch.await(getPage().getTimeout(), TimeUnit.SECONDS));

    // Wait until file exists
    waitForFileExists(recordingFile);

    AssertMedia.assertCodecs(recordingFile, expectedVideoCodec, expectedAudioCodec);
    mp.release();
  }

}
