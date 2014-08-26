/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.test.rabbitmq;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.factory.KurentoClient;
import org.kurento.client.factory.KurentoClientFactory;
import org.kurento.rabbitmq.client.JsonRpcClientRabbitMq;
import org.kurento.rabbitmq.server.JsonRpcServerRabbitMq;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.thrift.jsonrpcconnector.JsonRpcClientThrift;

/**
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class RabbitClientServer {

	@Test
	public void test() {

		KurentoServicesTestHelper.startKurentoMediaServer();

		KurentoClient mpf = KurentoClientFactory
				.createWithJsonRpcClient(new JsonRpcClientRabbitMq());

		JsonRpcServerRabbitMq server = new JsonRpcServerRabbitMq(
				new JsonRpcClientThrift());

		server.start();

		MediaPipeline pipeline = mpf.createMediaPipeline();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build();

		player.connect(httpGetEndpoint);

		String url = httpGetEndpoint.getUrl();

		player.release();

		Assert.assertNotSame("The URL shouldn't be empty", "", url);

		KurentoServicesTestHelper.teardownServices();

	}

}
