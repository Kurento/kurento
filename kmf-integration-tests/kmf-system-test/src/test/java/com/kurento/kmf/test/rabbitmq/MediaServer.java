package com.kurento.kmf.test.rabbitmq;

import com.kurento.kmf.common.Address;
import com.kurento.kmf.media.factory.KmfMediaApiProperties;
import com.kurento.kmf.rabbitmq.server.RabbitMqConnectorManager;

public class MediaServer {

	private RabbitMqConnectorManager mediaServerBroker;
	private int num;

	public MediaServer(int num) {
		this.num = num;
	}

	public void start() {

		Address thriftKmfAddress = KmfMediaApiProperties.getThriftKmfAddress();
		thriftKmfAddress.setPort(thriftKmfAddress.getPort() + num);

		mediaServerBroker = new RabbitMqConnectorManager(
				KmfMediaApiProperties.getThriftKmsAddress(), thriftKmfAddress,
				KmfMediaApiProperties.getRabbitMqAddress());

	}

	public void destroy() {
		mediaServerBroker.destroy();
	}

}
