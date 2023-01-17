/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.client.test;

import org.junit.Ignore;

@Ignore
public class RtpEndpointAsync2Test extends MediaPipelineAsyncBaseTest {

  // @Test
  // public void testStream() throws InterruptedException {
  //
  // AsyncResultManager<RtpEndpoint> async = new AsyncResultManager<>(
  // "RtpEndpoint creation");
  // new RtpEndpoint.Builder(pipeline).buildAsync(async.getContinuation());
  // RtpEndpoint rtp = async.waitForResult();
  //
  // AsyncResultManager<String> asyncGenerateOffer = new AsyncResultManager<>(
  // "rtp.generateOffer() invocation");
  // rtp.generateOffer(asyncGenerateOffer.getContinuation());
  // asyncGenerateOffer.waitForResult();
  //
  // AsyncResultManager<String> asyncProcessOffer = new AsyncResultManager<>(
  // "rtp.generateOffer() invocation");
  // rtp.processOffer("processOffer test",
  // asyncProcessOffer.getContinuation());
  // asyncProcessOffer.waitForResult();
  //
  // AsyncResultManager<String> asyncProcessAnswer = new AsyncResultManager<>(
  // "rtp.processAnswer() invocation");
  // rtp.processAnswer("processAnswer test",
  // asyncProcessAnswer.getContinuation());
  // asyncProcessAnswer.waitForResult();
  //
  // AsyncResultManager<String> asyncGetLocalSessionDescriptor = new
  // AsyncResultManager<>(
  // "rtp.getLocalSessionDescriptor() invocation");
  // rtp.getLocalSessionDescriptor(asyncGetLocalSessionDescriptor
  // .getContinuation());
  // asyncGetLocalSessionDescriptor.waitForResult();
  //
  // AsyncResultManager<String> asyncGetRemoteSessionDescriptor = new
  // AsyncResultManager<>(
  // "rtp.getRemoteSessionDescriptor() invocation");
  //
  // rtp.getRemoteSessionDescriptor(asyncGetRemoteSessionDescriptor
  // .getContinuation());
  // asyncGetRemoteSessionDescriptor.waitForResult();
  // }
  //
  // @Test
  // public void testSourceSinks() throws KurentoException,
  // InterruptedException {
  //
  // RtpEndpoint rtp = new RtpEndpoint.Builder(pipeline).build();
  //
  // AsyncResultManager<List<MediaSource>> asyncMediaSource = new
  // AsyncResultManager<>(
  // "rtp.getMediaSrcs() invocation");
  // rtp.getMediaSrcs(asyncMediaSource.getContinuation());
  // asyncMediaSource.waitForResult();
  //
  // AsyncResultManager<List<MediaSink>> asyncMediaSink = new
  // AsyncResultManager<>(
  // "rtp.getMediaSinks() invocation");
  // rtp.getMediaSinks(asyncMediaSink.getContinuation());
  // asyncMediaSink.waitForResult();
  //
  // AsyncResultManager<List<MediaSource>> asyncMediaSourceAudio = new
  // AsyncResultManager<>(
  // "rtp.getMediaSrcs(AUDIO) invocation");
  // rtp.getMediaSrcs(AUDIO, asyncMediaSourceAudio.getContinuation());
  // asyncMediaSourceAudio.waitForResult();
  //
  // AsyncResultManager<List<MediaSink>> asyncMediaSinkAudio = new
  // AsyncResultManager<>(
  // "rtp.getMediaSinks(AUDIO) invocation");
  // rtp.getMediaSinks(AUDIO, asyncMediaSinkAudio.getContinuation());
  // asyncMediaSinkAudio.waitForResult();
  //
  // rtp.release();
  // }
  //
  // @Test
  // public void testConnect() throws InterruptedException {
  //
  // PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, URL_SMALL)
  // .build();
  //
  // HttpEndpoint http = new HttpGetEndpoint.Builder(pipeline).build();
  //
  // AsyncResultManager<Void> async = new AsyncResultManager<>(
  // "player.connect() invocation");
  // player.connect(http, async.getContinuation());
  // async.waitForResult();
  //
  // player.play();
  // http.release();
  // player.release();
  // }
  //
  // @Test
  // public void testConnectByType() throws InterruptedException {
  // PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, URL_SMALL)
  // .build();
  // HttpEndpoint http = new HttpGetEndpoint.Builder(pipeline).build();
  //
  // AsyncResultManager<Void> asyncAudio = new AsyncResultManager<>(
  // "player.connect(AUDIO) invocation");
  // player.connect(http, AUDIO, asyncAudio.getContinuation());
  // asyncAudio.waitForResult();
  //
  // AsyncResultManager<Void> asyncVideo = new AsyncResultManager<>(
  // "player.connect() invocation");
  // player.connect(http, VIDEO, asyncVideo.getContinuation());
  // asyncVideo.waitForResult();
  //
  // player.play();
  // http.release();
  // player.release();
  // }

}