package org.kurento.kmf.media.factory;

import org.kurento.kmf.common.Address;
import org.kurento.kmf.common.PropertiesManager;

public class KmfMediaApiProperties {

	public static final String KMF_TRANSPORT_PROP = "kmf.transport";
	public static final String KMF_TRANSPORT_THRIFT_VALUE = "thrift";
	public static final String KMF_TRANSPORT_RABBITMQ_VALUE = "rabbitmq";
	public static final String KMF_TRANSPORT_WS_VALUE = "ws";
	public static final String KMF_TRANSPORT_DEFAULT = KMF_TRANSPORT_THRIFT_VALUE;

	public static final String THRIFT_KMS_ADDRESS_PROP = "thrift.kms.address";
	public static final Address THRIFT_KMS_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 9090);

	public static final String THRIFT_KMF_ADDRESS_PROP = "thrift.kmf.address";
	public static final Address THRIFT_KMF_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 9191);

	public static final String RABBITMQ_ADDRESS_PROP = "rabbitmq.address";
	public static final Address RABBITMQ_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 5672);

	public static final String WS_URI_PROP = "ws.uri";
	public static final String WS_URI_DEFAULT = "ws://localhost:7788/thrift";

	public static Address getThriftKmsAddress() {
		return getThriftKmsAddress(null);
	}

	public static Address getThriftKmsAddress(String prefix) {
		return PropertiesManager.getProperty(prefix, THRIFT_KMS_ADDRESS_PROP,
				THRIFT_KMS_ADDRESS_DEFAULT);
	}

	public static Address getThriftKmfAddress() {
		return getThriftKmfAddress(null);
	}

	public static Address getThriftKmfAddress(String prefix) {
		return PropertiesManager.getProperty(prefix, THRIFT_KMF_ADDRESS_PROP,
				THRIFT_KMF_ADDRESS_DEFAULT);
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
		return getKmfTransport(null);
	}

	public static String getKmfTransport(String prefix) {
		return PropertiesManager.getProperty(prefix, KMF_TRANSPORT_PROP,
				KMF_TRANSPORT_DEFAULT);
	}

}
