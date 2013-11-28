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

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * {@link RtpEndPoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RtpEndPoint#getLocalSessionDescriptor()}
 * <li>{@link RtpEndPoint#getRemoteSessionDescriptor()}
 * <li>{@link RtpEndPoint#generateOffer()}
 * <li>{@link RtpEndPoint#processOffer(String)}
 * <li>{@link RtpEndPoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link RtpEndPoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link RtpEndPoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 * 
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class RtpEndPointAsyncTest extends AbstractSdpAsyncBaseTest<RtpEndPoint> {

	@Before
	public void setup() throws InterruptedException {
		final BlockingQueue<RtpEndPoint> events = new ArrayBlockingQueue<RtpEndPoint>(
				1);
		pipeline.newRtpEndPoint().buildAsync(new Continuation<RtpEndPoint>() {

			@Override
			public void onSuccess(RtpEndPoint result) {
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
