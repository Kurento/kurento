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
 */
package org.kurento.client.test;

import org.junit.Assert;
import org.junit.Before;
import org.kurento.client.EventListener;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.client.test.util.SdpAsyncBaseTest;

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
 * <li>{@link WebRtcEndpoint#addMediaSessionStartedListener(EventListener)}
 * <li>
 * {@link WebRtcEndpoint#addMediaSessionTerminatedListener(EventListener)}
 * </ul>
 *
 *
 * @author Jose Antonio Santos Cadenas (santoscadenas@gmail.com)
 * @version 1.0.0
 *
 */
public class WebRtcEndpointAsyncTest extends SdpAsyncBaseTest<WebRtcEndpoint> {

	@Before
	public void setupMediaElements() {

		AsyncResultManager<WebRtcEndpoint> async = new AsyncResultManager<>(
				"RtpEndpoint creation");
		new WebRtcEndpoint.Builder(pipeline)
				.buildAsync(async.getContinuation());
		sdp = async.waitForResult();
		Assert.assertNotNull(sdp);

		AsyncResultManager<WebRtcEndpoint> async2 = new AsyncResultManager<>(
				"RtpEndpoint creation");
		new WebRtcEndpoint.Builder(pipeline).buildAsync(async2
				.getContinuation());
		sdp2 = async2.waitForResult();
		Assert.assertNotNull(sdp2);
	}

}
