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
package com.kurento.demo;

import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.HttpGetEndpoint;

/**
 * HTTP Player Handler which plays a RTP stream previously produced in
 * {@link RtpProducingJackVaderFilter} handler; using tunnelling strategy (by
 * default <code>redirect=true</code> in {@link HttpPlayerService} annotation;
 * using JSON signalling protocol.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see RtpProducingJackVaderFilter
 */
@HttpPlayerService(name = "PlayerConsumingRtpJackVaderFilter", path = "/playerRtpJack", useControlProtocol = true)
public class PlayerConsumingRtpJackVaderFilter extends HttpPlayerHandler {

	@Override
	public void onContentRequest(HttpPlayerSession session) throws Exception {
		getLogger().info("Received request to " + session);
		if (RtpProducingJackVaderFilter.sharedJackVaderReference != null) {
			getLogger()
					.info("Found sharedJackVaderReference ... invoking play");
			HttpGetEndpoint httpEP = session.getMediaPipelineFactory().create()
					.newHttpGetEndpoint().terminateOnEOS().build();
			RtpProducingJackVaderFilter.sharedJackVaderReference
					.connect(httpEP);
			session.start(httpEP);
		} else {
			getLogger()
					.info("Cannot find sharedJackVaderReference instance ... rejecting request");
			session.terminate(500,
					"Cannot find sharedJackVaderReference instance ... rejecting request");
		}
	}
}
