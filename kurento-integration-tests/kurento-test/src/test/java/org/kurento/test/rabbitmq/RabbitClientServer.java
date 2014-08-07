package org.kurento.test.rabbitmq;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.factory.KurentoClient;
import org.kurento.rabbitmq.client.JsonRpcClientRabbitMq;
import org.kurento.rabbitmq.server.JsonRpcServerRabbitMq;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.thrift.jsonrpcconnector.JsonRpcClientThrift;

public class RabbitClientServer {

	@Test
	public void test() {

		KurentoServicesTestHelper.startKurentoMediaServer();

		KurentoClient mpf = new KurentoClient(
				new JsonRpcClientRabbitMq());

		JsonRpcServerRabbitMq server = new JsonRpcServerRabbitMq(
				new JsonRpcClientThrift());

		server.start();

		MediaPipeline pipeline = mpf.createMediaPipeline();

		PlayerEndpoint player = pipeline.newPlayerEndpoint(
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = pipeline.newHttpGetEndpoint().build();

		player.connect(httpGetEndpoint);

		String url = httpGetEndpoint.getUrl();

		player.release();

		Assert.assertNotSame("The URL shouldn't be empty", "", url);

		KurentoServicesTestHelper.teardownServices();

	}

}
