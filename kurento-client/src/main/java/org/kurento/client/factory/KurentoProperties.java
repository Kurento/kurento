package org.kurento.client.factory;

import org.kurento.commons.Address;
import org.kurento.commons.PropertiesManager;

public class KurentoProperties {

	public static final String KURENTO_CLIENT_TRANSPORT_PROP = "kmf.transport";
	public static final String KURENTO_CLIENT_TRANSPORT_THRIFT_VALUE = "thrift";
	public static final String KURENTO_CLIENT_TRANSPORT_RABBITMQ_VALUE = "rabbitmq";
	public static final String KURENTO_CLIENT_TRANSPORT_WS_VALUE = "ws";
	public static final String KURENTO_CLIENT_TRANSPORT_DEFAULT = KURENTO_CLIENT_TRANSPORT_THRIFT_VALUE;

	public static final String THRIFT_KMS_ADDRESS_PROP = "thrift.kms.address";
	public static final Address THRIFT_KMS_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 9090);

	public static final String THRIFT_KCS_ADDRESS_PROP = "thrift.kmf.address";
	public static final Address THRIFT_KCS_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 9191);

	public static final String RABBITMQ_ADDRESS_PROP = "rabbitmq.address";
	public static final Address RABBITMQ_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 5672);

	public static final String WS_URI_PROP = "ws.uri";
	public static final String WS_URI_DEFAULT = "ws://localhost:8888/kurento";

	public static Address getThriftKmsAddress() {
		return getThriftKmsAddress(null);
	}

	public static Address getThriftKmsAddress(String prefix) {
		return PropertiesManager.getProperty(prefix, THRIFT_KMS_ADDRESS_PROP,
				THRIFT_KMS_ADDRESS_DEFAULT);
	}

	public static Address getThriftKcsAddress() {
		return getThriftKcsAddress(null);
	}

	public static Address getThriftKcsAddress(String prefix) {
		return PropertiesManager.getProperty(prefix, THRIFT_KCS_ADDRESS_PROP,
				THRIFT_KCS_ADDRESS_DEFAULT);
	}

	public static Address getRabbitMqAddress() {
		return getRabbitMqAddress(null);
	}

	public static Address getRabbitMqAddress(String prefix) {
		return PropertiesManager.getProperty(prefix, RABBITMQ_ADDRESS_PROP,
				RABBITMQ_ADDRESS_DEFAULT);
	}

	public static String getWsUri() {
		return getWsUri(null);
	}

	public static String getWsUri(String prefix) {
		return PropertiesManager.getProperty(prefix, WS_URI_PROP,
				WS_URI_DEFAULT);
	}

	public static String getKmfTransport() {
		return getKurentoClientTransport(null);
	}

	public static String getKurentoClientTransport(String prefix) {
		return PropertiesManager
				.getProperty(prefix, KURENTO_CLIENT_TRANSPORT_PROP,
						KURENTO_CLIENT_TRANSPORT_DEFAULT);
	}

}
