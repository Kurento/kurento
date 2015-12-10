/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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