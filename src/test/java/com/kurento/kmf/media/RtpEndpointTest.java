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

import org.junit.After;
import org.junit.Before;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * {@link RtpEndpoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link RtpEndpoint#getLocalSessionDescriptor()}
 * <li>{@link RtpEndpoint#getRemoteSessionDescriptor()}
 * <li>{@link RtpEndpoint#generateOffer()}
 * <li>{@link RtpEndpoint#processOffer(String)}
 * <li>{@link RtpEndpoint#processAnswer(String)}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link RtpEndpoint#addMediaSessionStartedListener(MediaEventListener)}
 * <li>
 * {@link RtpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 * 
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class RtpEndpointTest extends AbstractSdpBaseTest<RtpEndpoint> {

	@Before
	public void setup() throws KurentoMediaFrameworkException {
		sdp = pipeline.newRtpEndpoint().build();
		sdp2 = pipeline.newRtpEndpoint().build();
	}

	@After
	public void teardown() {
		sdp.release();
		sdp2.release();
	}
}
