package com.kurento.kmf.test.rabbitmq;

import org.junit.Assert;
import org.junit.Test;

import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;
import com.kurento.kmf.rabbitmq.client.JsonRpcClientRabbitMq;
import com.kurento.kmf.rabbitmq.server.JsonRpcServerRabbitMq;
import com.kurento.kmf.test.services.KurentoServicesTestHelper;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;

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
