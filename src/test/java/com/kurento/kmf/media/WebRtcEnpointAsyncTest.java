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
package com.kurento.kmf.media;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * {@link WebRtcEndpoint} test suite.
 * 
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
// TODO enable when WebRTC type is recognised by server
@Ignore
public class WebRtcEnpointAsyncTest extends
		AbstractSdpAsyncBaseTest<WebRtcEndpoint> {

	@Before
	public void setup() throws InterruptedException {
		final BlockingQueue<WebRtcEndpoint> events = new ArrayBlockingQueue<WebRtcEndpoint>(
				1);
		pipeline.newWebRtcEndpoint().buildAsync(
				new Continuation<WebRtcEndpoint>() {

					@Override
					public void onSuccess(WebRtcEndpoint result) {
						events.add(result);
					}

					@Override
					public void onError(Throwable cause) {
						throw new KurentoMediaFrameworkException(cause);
					}
				});
		sdp = events.poll(500, MILLISECONDS);
		Assert.assertNotNull(sdp);
	}

	@After
	public void teardown() throws InterruptedException {
		releaseMediaObject(sdp);
	}
}
