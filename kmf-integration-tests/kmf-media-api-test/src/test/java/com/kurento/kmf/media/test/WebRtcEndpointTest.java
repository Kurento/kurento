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
package com.kurento.kmf.media.test;

import org.junit.Before;

import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.test.base.SdpBaseTest;

/**
 * {@link WebRtcEndpoint} test suite.
 *
 *
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 *
 */
public class WebRtcEndpointTest extends SdpBaseTest<WebRtcEndpoint> {

	@Before
	public void setupMediaElements() {
		sdp = pipeline.newWebRtcEndpoint().build();
		sdp2 = pipeline.newWebRtcEndpoint().build();
	}

}
