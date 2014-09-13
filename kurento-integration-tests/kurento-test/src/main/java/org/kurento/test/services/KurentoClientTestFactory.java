package org.kurento.test.services;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.services.KurentoServicesTestHelper.KCS_WS_URI_PROP;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_RABBITMQ_ADDRESS_DEFAULT;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_RABBITMQ_ADDRESS_PROP;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_TRANSPORT_DEFAULT;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_TRANSPORT_PROP;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_TRANSPORT_RABBITMQ_VALUE;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_TRANSPORT_WS_VALUE;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_WS_URI_DEFAULT;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_WS_URI_PROP;

import java.lang.reflect.Constructor;
import java.security.InvalidParameterException;

import org.kurento.client.factory.KurentoClient;
import org.kurento.commons.Address;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KurentoClientTestFactory {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoClientTestFactory.class);

	public static KurentoClient createKurentoForTest() {
		return KurentoClient
				.createFromJsonRpcClient(createJsonRpcClient("client"));
	}

	public static JsonRpcClient createJsonRpcClient(String prefix) {

		String kmsTransport;

		if (prefix.equals("client")) {

			if (getProperty(KCS_WS_URI_PROP) != null) {

				String wsUri = getProperty(KCS_WS_URI_PROP);

				log.info(
						"Connecting kurento client to kcs with websockets to uri '{}'",
						wsUri);

				return new JsonRpcClientWebSocket(wsUri);
			} else {

				return createJsonRpcClient("kcs");
			}

		} else if (prefix.equals("kcs")) {

			kmsTransport = getProperty(KMS_TRANSPORT_PROP,
					KMS_TRANSPORT_DEFAULT);

			switch (kmsTransport) {
			case KMS_TRANSPORT_WS_VALUE:

				String wsUri = getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT);

				log.info("Connecting kcs to kms with websockets to uri '{}'",
						wsUri);

				return new JsonRpcClientWebSocket(wsUri);

			case KMS_TRANSPORT_RABBITMQ_VALUE:

				Address rabbitAddress = getProperty(KMS_RABBITMQ_ADDRESS_PROP,
						KMS_RABBITMQ_ADDRESS_DEFAULT);

				log.info("Connecting kcs to kms with RabbitMQ in address '{}'",
						rabbitAddress);

				return newJsonRpcClientRabbitMq(rabbitAddress);

			default:

				throw new RuntimeException(
						"Invalid transport value in property '"
								+ KMS_TRANSPORT_PROP + "': " + kmsTransport
								+ ". Valid values are: "
								+ KMS_TRANSPORT_RABBITMQ_VALUE + " or "
								+ KMS_TRANSPORT_WS_VALUE);
			}
		} else {
			throw new InvalidParameterException();
		}
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

	public static KurentoClient createWithJsonRpcClient(JsonRpcClient client) {
		return KurentoClient.createFromJsonRpcClient(client);
	}

}
