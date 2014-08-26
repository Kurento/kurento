package org.kurento.test.services;

import static org.kurento.client.factory.KurentoProperties.getRabbitMqAddress;
import static org.kurento.client.factory.KurentoProperties.getThriftKcsAddress;
import static org.kurento.client.factory.KurentoProperties.getThriftKmsAddress;
import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.kurento.client.factory.KurentoClientFactory;
import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.rabbitmq.server.RabbitMqConnectorManager;
import org.kurento.test.Shell;

public class KurentoServicesTestHelper {

	public static final String KMS_TRANSPORT_PROP = "kms.transport";
	public static final String KMS_TRANSPORT_THRIFT_VALUE = "thrift";
	public static final String KMS_TRANSPORT_RABBITMQ_VALUE = "rabbitmq";
	public static final String KMS_TRANSPORT_DEFAULT = KMS_TRANSPORT_THRIFT_VALUE;

	public static final String KMS_PRINT_LOG_PROP = "kms.print.log";
	public static final String KMS_PRINT_LOG_DEFAULT = "true";

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

	public static final String KURENTO_TESTFILES_PROP = "kurento.test.files";
	public static final String KURENTO_TESTFILES_DEFAULT = "/var/lib/jenkins/test-files";

	private static final String PROJECT_PATH_PROP = "project.path";
	private static final String PROJECT_PATH_DEFAULT = ".";

	private static HttpServer httpServer;
	private static KurentoMediaServerManager kms;
	private static RabbitMqConnectorManager rabbitMqConnector;
	private static KurentoControlServerManager mediaConnector;

	private static String testCaseName;
	private static String testName;
	private static String testDir;
	private static String kmsAutostart = KMS_AUTOSTART_DEFAULT;
	private static String krcAutostart = KRC_AUTOSTART_DEFAULT;
	private static String kmcAutostart = KMS_AUTOSTART_DEFAULT;
	private static String kmsPrintLog;
	private static File logFile;

	public static void startKurentoServicesIfNeccessary() {

		startKurentoMediaServerIfNecessary();
		startRabbitMqConnectorIfNecessary();
		startKurentoControlServerIfNecessary();
	}

	private static void startKurentoControlServerIfNecessary() {

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
		kmsPrintLog = getProperty(KMS_PRINT_LOG_PROP, KMS_PRINT_LOG_DEFAULT);
		testDir = getProperty(PROJECT_PATH_PROP, PROJECT_PATH_DEFAULT)
				+ "/target/surefire-reports/";

		String logFolder = testDir + testCaseName;
		createFolder(logFolder);

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
				getThriftKcsAddress(RABBITMQ_CONNECTOR_PREFIX),
				getRabbitMqAddress(RABBITMQ_CONNECTOR_PREFIX));
	}

	private static void startMediaConnector() {

		JsonRpcClient client = KurentoClientFactory
				.createJsonRpcClient(MEDIA_CONNECTOR_PREFIX);

		mediaConnector = new KurentoControlServerManager(client,
				getKmcHttpPort());
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

		kms.setTestClassName(testCaseName);
		kms.setTestMethodName(testName);
		kms.setTestDir(testDir);
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

	public static void teardownHttpServer() {
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

	public static String getTestName() {
		return KurentoServicesTestHelper.testName;
	}

	public static void setTestCaseName(String testCaseName) {
		KurentoServicesTestHelper.testCaseName = testCaseName;
	}

	public static String getTestCaseName() {
		return KurentoServicesTestHelper.testCaseName;
	}

	public static void setTestDir(String testDir) {
		KurentoServicesTestHelper.testDir = testDir;
	}

	public static String getTestDir() {
		return KurentoServicesTestHelper.testDir;
	}

	public static boolean printKmsLog() {
		return Boolean.parseBoolean(kmsPrintLog);
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

	public static String getTestFilesPath() {
		return PropertiesManager.getProperty(KURENTO_TESTFILES_PROP,
				KURENTO_TESTFILES_DEFAULT);
	}

	public static void setServerLogFilePath(File logFile) {
		KurentoServicesTestHelper.logFile = logFile;
	}

	public static File getServerLogFile() {
		return logFile;
	}

	private static void createFolder(String folder) {
		File folderFile = new File(folder);
		if (!folderFile.exists()) {
			folderFile.mkdirs();
		}
		Shell.run("chmod", "a+w", folder);
	}
}
