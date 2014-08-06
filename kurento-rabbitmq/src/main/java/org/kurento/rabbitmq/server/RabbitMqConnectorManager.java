package org.kurento.rabbitmq.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.commons.Address;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.rabbitmq.RabbitMqException;
import org.kurento.thrift.jsonrpcconnector.JsonRpcClientThrift;

public class RabbitMqConnectorManager {

	private static final Logger log = LoggerFactory
			.getLogger(RabbitMqConnectorManager.class);

	private JsonRpcServerRabbitMq rabbitMqToThriftConnector;

	private JsonRpcClient client;

	public RabbitMqConnectorManager(Address thriftKmsAddress,
			Address thriftKmfAddress, Address rabbitMqAddress) {

		this.client = new JsonRpcClientThrift(thriftKmsAddress.getHost(),
				thriftKmsAddress.getPort(), thriftKmfAddress.getHost(),
				thriftKmfAddress.getPort());

		log.info("Starting RabbitMQ to Thrift Media Connector");

		this.rabbitMqToThriftConnector = new JsonRpcServerRabbitMq(client,
				rabbitMqAddress);
		this.rabbitMqToThriftConnector.start();

		log.info("RabbitMQ to Thrift Media Connector started");
	}

	public void destroy() {

		try {
			this.client.close();
			this.rabbitMqToThriftConnector.destroy();
		} catch (IOException e) {
			throw new RabbitMqException(
					"Exception while destroying MediaServerBroker", e);
		}
	}
}
