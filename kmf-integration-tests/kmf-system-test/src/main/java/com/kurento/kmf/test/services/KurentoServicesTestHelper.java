package com.kurento.kmf.test.services;

import static com.kurento.kmf.common.PropertiesManager.getProperty;
import static com.kurento.kmf.media.factory.KmfMediaApiProperties.*;

import org.apache.catalina.LifecycleException;

import com.kurento.kmf.common.PropertiesManager;
import com.kurento.kmf.connector.MediaConnectorManager;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.media.factory.KmfMediaApi;
import com.kurento.kmf.rabbitmq.server.RabbitMqConnectorManager;

public class KurentoServicesTestHelper {

	public static final String KMS_TRANSPORT_PROP = "kms.transport";
	public static final String KMS_TRANSPORT_THRIFT_VALUE = "thrift";
	public static final String KMS_TRANSPORT_RABBITMQ_VALUE = "rabbitmq";
	public static final String KMS_TRANSPORT_DEFAULT = KMS_TRANSPORT_THRIFT_VALUE;

	public static final String AUTOSTART_FALSE_VALUE = "false";
	public static final String AUTOSTART_TEST_VALUE = "test";
	public static final String AUTOSTART_TEST_SUITE_VALUE = "testsuite";

	public static final String KMS_AUTOSTART_PROP = "kms.autostart";
	public static final String KMS_AUTOSTART_DEFAULT = AUTOSTART_TEST_VALUE;

	public static final String KMC_AUTOSTART_PROP = "kmc.autostart";
	public static final String KMC_AUTOSTART_DEFAULT = AUTOSTART_FALSE_VALUE;

	public static final String KRC_AUTOSTART_PROP = "krc.autostart";
	public static final String KRC_AUTOSTART_DEFAULT = AUTOSTART_FALSE_VALUE;

	public static final String KMS_HTTP_PORT_PROP = "kms.http.port";
	public static final int KMS_HTTP_PORT_DEFAULT = 9091;

	public static final String APP_HTTP_PORT_PROP = "app.http.port";
	public static final int APP_HTTP_PORT_DEFAULT = 7779;

	public static final String KMC_HTTP_PORT_PROP = "kmc.http.port";
	public static final int KMC_HTTP_PORT_DEFAULT = 7788;

	public static final String MEDIA_CONNECTOR_PREFIX = "kmc";
	public static final String RABBITMQ_CONNECTOR_PREFIX = "krc";

	private static HttpServer httpServer;
	private static KurentoMediaServerManager kms;
	private static RabbitMqConnectorManager rabbitMqConnector;
	private static MediaConnectorManager mediaConnector;

	private static String testName;
	private static String kmsAutostart;
	private static String krcAutostart;
	private static String kmcAutostart;

	public static void startKurentoServicesIfNeccessary() {

		startKurentoMediaServerIfNecessary();
		startRabbitMqConnectorIfNecessary();
		startMediaConnectorIfNecessary();
	}

	private static void startMediaConnectorIfNecessary() {

		kmcAutostart = getProperty(KMC_AUTOSTART_PROP, KMC_AUTOSTART_DEFAULT);

		switch (kmcAutostart) {
		case AUTOSTART_FALSE_VALUE:
			break;
		case AUTOSTART_TEST_VALUE:
			startMediaConnector();
			break;
		case AUTOSTART_TEST_SUITE_VALUE:
			if (mediaConnector == null) {
				startMediaConnector();
			}
			break;
		default:
			throw new IllegalArgumentException("The value '" + kmcAutostart
					+ "' is not valid for property " + KMC_AUTOSTART_PROP);
		}
	}

	private static void startRabbitMqConnectorIfNecessary() {
		krcAutostart = getProperty(KRC_AUTOSTART_PROP, KRC_AUTOSTART_DEFAULT);

		switch (krcAutostart) {
		case AUTOSTART_FALSE_VALUE:
			break;
		case AUTOSTART_TEST_VALUE:
			startRabbitMqConnector();
			break;
		case AUTOSTART_TEST_SUITE_VALUE:
			if (mediaConnector == null) {
				startRabbitMqConnector();
			}
			break;
		default:
			throw new IllegalArgumentException("The value '" + krcAutostart
					+ "' is not valid for property " + KRC_AUTOSTART_PROP);
		}
	}

	private static void startKurentoMediaServerIfNecessary() {
		kmsAutostart = getProperty(KMS_AUTOSTART_PROP, KMS_AUTOSTART_DEFAULT);

		switch (kmsAutostart) {
		case AUTOSTART_FALSE_VALUE:
			break;
		case AUTOSTART_TEST_VALUE:
			startKurentoMediaServer();
			break;
		case AUTOSTART_TEST_SUITE_VALUE:
			if (mediaConnector == null) {
				startKurentoMediaServer();
			}
			break;
		default:
			throw new IllegalArgumentException("The value '" + kmsAutostart
					+ "' is not valid for property " + KMS_AUTOSTART_PROP);
		}
	}

	private static void startRabbitMqConnector() {

		rabbitMqConnector = new RabbitMqConnectorManager(
				getThriftKmsAddress(RABBITMQ_CONNECTOR_PREFIX),
				getThriftKmfAddress(RABBITMQ_CONNECTOR_PREFIX),
				getRabbitMqAddress(RABBITMQ_CONNECTOR_PREFIX));
	}

	private static void startMediaConnector() {

		JsonRpcClient client = KmfMediaApi
				.createJsonRpcClientFromSystemProperties(MEDIA_CONNECTOR_PREFIX);

		mediaConnector = new MediaConnectorManager(client, getKmcHttpPort());
	}

	public static void startKurentoMediaServer() {

		String transport = PropertiesManager.getProperty(KMS_TRANSPORT_PROP,
				KMS_TRANSPORT_DEFAULT);

		int httpPort = getKmsHttpPort();

		switch (transport) {
		case KMS_TRANSPORT_THRIFT_VALUE:

			kms = KurentoMediaServerManager.createWithThriftTransport(
					getThriftKmsAddress(), httpPort);
			break;
		case KMS_TRANSPORT_RABBITMQ_VALUE:

			kms = KurentoMediaServerManager.createWithRabbitMqTransport(
					getRabbitMqAddress(), httpPort);
			break;

		default:
			throw new IllegalArgumentException("The value " + transport
					+ " is not valid for property " + KMS_TRANSPORT_PROP);
		}

		kms.setLogFolder(testName);
		kms.start();
	}

	public static void startHttpServer() {
		try {
			httpServer = new HttpServer(getAppHttpPort());
			httpServer.start();
		} catch (Exception e) {
			throw new RuntimeException("Exception starting http server", e);
		}
	}

	public static void teardownServices() {

		teardownHttpServer();
		teardownMediaServer();
		teardownMediaConnector();
		teardownRabbitMediaConnector();
	}

	private static void teardownMediaConnector() {
		if (mediaConnector != null && kmcAutostart.equals(AUTOSTART_TEST_VALUE)) {
			mediaConnector.destroy();
			mediaConnector = null;
		}
	}

	private static void teardownRabbitMediaConnector() {
		if (rabbitMqConnector != null
				&& krcAutostart.equals(AUTOSTART_TEST_VALUE)) {
			rabbitMqConnector.destroy();
			rabbitMqConnector = null;
		}
	}

	private static void teardownMediaServer() {
		if (kms != null && kmsAutostart.equals(AUTOSTART_TEST_VALUE)) {
			kms.stop();
			kms = null;
		}
	}

	private static void teardownHttpServer() {
		if (httpServer != null) {
			try {
				httpServer.destroy();
			} catch (LifecycleException e) {
				e.printStackTrace();
			}
		}
	}

	public static void setTestName(String testName) {
		KurentoServicesTestHelper.testName = testName;
	}

	public static int getKmsHttpPort() {
		return PropertiesManager.getProperty(KMS_HTTP_PORT_PROP,
				KMS_HTTP_PORT_DEFAULT);
	}

	public static int getAppHttpPort() {
		return PropertiesManager.getProperty(APP_HTTP_PORT_PROP,
				APP_HTTP_PORT_DEFAULT);
	}

	public static int getKmcHttpPort() {
		return PropertiesManager.getProperty(KMC_HTTP_PORT_PROP,
				KMC_HTTP_PORT_DEFAULT);
	}
}
