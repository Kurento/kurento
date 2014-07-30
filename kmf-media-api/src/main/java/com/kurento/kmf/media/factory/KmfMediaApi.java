package com.kurento.kmf.media.factory;

import static com.kurento.kmf.media.factory.KmfMediaApiProperties.KMF_TRANSPORT_PROP;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.KMF_TRANSPORT_RABBITMQ_VALUE;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.KMF_TRANSPORT_THRIFT_VALUE;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.KMF_TRANSPORT_WS_VALUE;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.getKmfTransport;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.getRabbitMqAddress;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.getThriftKmfAddress;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.getThriftKmsAddress;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.getWsUri;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.Address;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClientWebSocket;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;

public class KmfMediaApi {

	private static final Logger log = LoggerFactory
			.getLogger(KmfMediaApi.class);

	public static MediaPipelineFactory createMediaPipelineFactoryFromSystemProps() {
		return createMediaPipelineFactoryFromSystemProps(null);
	}

	public static MediaPipelineFactory createMediaPipelineFactoryFromSystemProps(
			String prefix) {

		return new MediaPipelineFactory(
				createJsonRpcClientFromSystemProperties(prefix));
	}

	public static JsonRpcClient createJsonRpcClientFromSystemProperties() {
		return createJsonRpcClientFromSystemProperties(null);
	}

	@SuppressWarnings("resource")
	public static JsonRpcClient createJsonRpcClientFromSystemProperties(
			String prefix) {

		JsonRpcClient client;

		String kmfTransport = getKmfTransport(prefix);

		switch (kmfTransport) {
		case KMF_TRANSPORT_THRIFT_VALUE:

			log.info(
					"Creating JsonRpcClient with Thrift transport. Prefix: {}",
					prefix);

			Address kmsAddress = getThriftKmsAddress(prefix);
			Address kmfAddress = getThriftKmfAddress(prefix);

			log.info("kmsThriftAddress: {} and kmfThriftAddress: {}",
					kmsAddress, kmfAddress);

			client = new JsonRpcClientThrift(kmsAddress.getHost(),
					kmsAddress.getPort(), kmfAddress.getHost(),
					kmfAddress.getPort());

			break;

		case KMF_TRANSPORT_WS_VALUE:

			log.info(
					"Creating JsonRpcClient with WebSocket transport. Prefix: {}",
					prefix);

			client = new JsonRpcClientWebSocket(getWsUri(prefix));

			break;

		case KMF_TRANSPORT_RABBITMQ_VALUE:

			log.info(
					"Creating JsonRpcClient with RabbitMQ transport. Prefix: {}",
					prefix);

			client = newJsonRpcClientRabbitMq(getRabbitMqAddress(prefix));

			break;

		default:

			throw new RuntimeException("Invalid transport value in property '"
					+ KMF_TRANSPORT_PROP + "': " + kmfTransport
					+ ". Valid values are: " + KMF_TRANSPORT_THRIFT_VALUE
					+ ", " + KMF_TRANSPORT_RABBITMQ_VALUE + " or "
					+ KMF_TRANSPORT_WS_VALUE);

		}

		return client;
	}

	private static JsonRpcClient newJsonRpcClientRabbitMq(
			Address rabbitMqAddress) {

		try {

			@SuppressWarnings("unchecked")
			Class<? extends JsonRpcClient> clazz = (Class<? extends JsonRpcClient>) Class
					.forName("com.kurento.kmf.rabbitmq.client.JsonRpcClientRabbitMq");

			Constructor<? extends JsonRpcClient> constructor = clazz
					.getConstructor(Address.class);

			return constructor.newInstance(rabbitMqAddress);

		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"MediaPipelineFactory is configured to use RabbitMQ but class "
							+ "JsonRpcClientRabbitMq is not in the classpath. Plase review "
							+ "you have correctly configured the dependency with kmf-rabbitmq project.",
					e);

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception creating JsonRpcClientRabbitMq", e);
		}
	}

}
