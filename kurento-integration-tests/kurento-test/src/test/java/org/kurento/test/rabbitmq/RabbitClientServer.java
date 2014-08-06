package org.kurento.test.rabbitmq;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.factory.MediaPipelineFactory;
import org.kurento.rabbitmq.client.JsonRpcClientRabbitMq;
import org.kurento.rabbitmq.server.JsonRpcServerRabbitMq;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.thrift.jsonrpcconnector.JsonRpcClientThrift;

public class RabbitClientServer {

	@Test
	public void test() {

		KurentoServicesTestHelper.startKurentoMediaServer();

		MediaPipelineFactory mpf = new MediaPipelineFactory(
				new JsonRpcClientRabbitMq());

		JsonRpcServerRabbitMq server = new JsonRpcServerRabbitMq(
				new JsonRpcClientThrift());

		server.start();

		MediaPipeline pipeline = mpf.create();

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
