package org.kurento.client.factory;

import static org.kurento.client.factory.KurentoProperties.KURENTO_CLIENT_TRANSPORT_PROP;
import static org.kurento.client.factory.KurentoProperties.KURENTO_CLIENT_TRANSPORT_RABBITMQ_VALUE;
import static org.kurento.client.factory.KurentoProperties.KURENTO_CLIENT_TRANSPORT_THRIFT_VALUE;
import static org.kurento.client.factory.KurentoProperties.KURENTO_CLIENT_TRANSPORT_WS_VALUE;
import static org.kurento.client.factory.KurentoProperties.getKurentoClientTransport;
import static org.kurento.client.factory.KurentoProperties.getRabbitMqAddress;
import static org.kurento.client.factory.KurentoProperties.getThriftKcsAddress;
import static org.kurento.client.factory.KurentoProperties.getThriftKmsAddress;
import static org.kurento.client.factory.KurentoProperties.getWsUri;

import java.lang.reflect.Constructor;

import org.kurento.commons.Address;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.thrift.jsonrpcconnector.JsonRpcClientThrift;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KurentoClientFactory {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoClientFactory.class);

	public static KurentoClient createKurentoClient() {
		return createKurentoClient(null);
	}

	public static KurentoClient createKurentoClient(String prefix) {

		return new KurentoClient(createJsonRpcClient(prefix));
	}

	public static JsonRpcClient createJsonRpcClient() {
		return createJsonRpcClient(null);
	}

	@SuppressWarnings("resource")
	public static JsonRpcClient createJsonRpcClient(String prefix) {

		JsonRpcClient client;

		String kurentoClientTransport = getKurentoClientTransport(prefix);

		switch (kurentoClientTransport) {
		case KURENTO_CLIENT_TRANSPORT_THRIFT_VALUE:

			log.info(
					"Creating JsonRpcClient with Thrift transport. Prefix: {}",
					prefix);

			Address kmsAddress = getThriftKmsAddress(prefix);
			Address kcsAddress = getThriftKcsAddress(prefix);

			log.info("kmsThriftAddress: {} and kmfThriftAddress: {}",
					kmsAddress, kcsAddress);

			client = new JsonRpcClientThrift(kmsAddress.getHost(),
					kmsAddress.getPort(), kcsAddress.getHost(),
					kcsAddress.getPort());

			break;

		case KURENTO_CLIENT_TRANSPORT_WS_VALUE:

			log.info(
					"Creating JsonRpcClient with WebSocket transport. Prefix: {}",
					prefix);

			client = new JsonRpcClientWebSocket(getWsUri(prefix));

			break;

		case KURENTO_CLIENT_TRANSPORT_RABBITMQ_VALUE:

			log.info(
					"Creating JsonRpcClient with RabbitMQ transport. Prefix: {}",
					prefix);

			client = newJsonRpcClientRabbitMq(getRabbitMqAddress(prefix));

			break;

		default:

			throw new RuntimeException("Invalid transport value in property '"
					+ KURENTO_CLIENT_TRANSPORT_PROP + "': " + kurentoClientTransport
					+ ". Valid values are: " + KURENTO_CLIENT_TRANSPORT_THRIFT_VALUE
					+ ", " + KURENTO_CLIENT_TRANSPORT_RABBITMQ_VALUE + " or "
					+ KURENTO_CLIENT_TRANSPORT_WS_VALUE);

		}

		return client;
	}

	private static JsonRpcClient newJsonRpcClientRabbitMq(
			Address rabbitMqAddress) {

		try {

			@SuppressWarnings("unchecked")
			Class<? extends JsonRpcClient> clazz = (Class<? extends JsonRpcClient>) Class
					.forName("org.kurento.rabbitmq.client.JsonRpcClientRabbitMq");

			Constructor<? extends JsonRpcClient> constructor = clazz
					.getConstructor(Address.class);

			return constructor.newInstance(rabbitMqAddress);

		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"KurentoClient is configured to use RabbitMQ but class "
							+ "JsonRpcClientRabbitMq is not in the classpath. Plase review "
							+ "you have correctly configured the dependency with kurento-rabbitmq project.",
					e);

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception creating JsonRpcClientRabbitMq", e);
		}
	}

}
