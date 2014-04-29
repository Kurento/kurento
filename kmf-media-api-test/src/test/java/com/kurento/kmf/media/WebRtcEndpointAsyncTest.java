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

import static java.util.concurrent.TimeUnit.SECONDS;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.kurento.kmf.media.events.MediaEventListener;

/**
 * {@link WebRtcEndpoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#getLocalSessionDescriptor()}
 * <li>{@link WebRtcEndpoint#getRemoteSessionDescriptor()}
 * <li>{@link WebRtcEndpoint#generateOffer()}
 * <li>{@link WebRtcEndpoint#processOffer(String)}
 * <li>{@link WebRtcEndpoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#addMediaSessionStartedListener(MediaEventListener)}
 * <li>
 * {@link WebRtcEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 * 
 * 
 * @author Jose Antonio Santos Cadenas (santoscadenas@gmail.com)
 * @version 1.0.0
 * 
 */
public class WebRtcEndpointAsyncTest extends
		AbstractSdpAsyncBaseTest<WebRtcEndpoint> {

	@Before
	public void setup() throws InterruptedException {
		pipeline.newWebRtcEndpoint().buildAsync(cont);
		pipeline.newWebRtcEndpoint().buildAsync(cont);

		sdp = creationResults.poll(1, SECONDS);
		sdp2 = creationResults.poll(1, SECONDS);
		Assert.assertNotNull(sdp);
		Assert.assertNotNull(sdp2);
	}

	@After
	public void teardown() throws InterruptedException {
		releaseMediaObject(sdp);
		releaseMediaObject(sdp2);
	}

}
