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
package org.kurento.client.factory;

import java.io.IOException;

import javax.annotation.PreDestroy;

import org.kurento.client.MediaPipeline;

/**
 * Factory to create {@link MediaPipeline} in the media server.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
@Deprecated
public class KurentoClient {

	private org.kurento.client.KurentoClient kurentoClient;

	public KurentoClient(org.kurento.client.KurentoClient kurentoClient) {
		this.kurentoClient = kurentoClient;
	}

	public static KurentoClient create(String websocketUrl) {
		try {
			return new KurentoClient(
					org.kurento.client.KurentoClient.create(websocketUrl));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@PreDestroy
	public void destroy() {
		kurentoClient.destroy();
	}

	@Deprecated
	public MediaPipeline createMediaPipeline() {
		MediaPipeline pipeline = MediaPipeline.with(kurentoClient).create();

		return pipeline;
	}
}