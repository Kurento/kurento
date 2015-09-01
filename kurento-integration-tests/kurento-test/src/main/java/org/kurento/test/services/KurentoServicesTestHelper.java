/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.test.services;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.APP_HTTP_PORT_DEFAULT;
import static org.kurento.test.TestConfiguration.APP_HTTP_PORT_PROP;
import static org.kurento.test.TestConfiguration.AUTOSTART_FALSE_VALUE;
import static org.kurento.test.TestConfiguration.AUTOSTART_TEST_SUITE_VALUE;
import static org.kurento.test.TestConfiguration.AUTOSTART_TEST_VALUE;
import static org.kurento.test.TestConfiguration.BOWER_KURENTO_CLIENT_TAG_DEFAULT;
import static org.kurento.test.TestConfiguration.BOWER_KURENTO_CLIENT_TAG_PROP;
import static org.kurento.test.TestConfiguration.BOWER_KURENTO_UTILS_TAG_DEFAULT;
import static org.kurento.test.TestConfiguration.BOWER_KURENTO_UTILS_TAG_PROP;
import static org.kurento.test.TestConfiguration.KCS_AUTOSTART_DEFAULT;
import static org.kurento.test.TestConfiguration.KCS_AUTOSTART_PROP;
import static org.kurento.test.TestConfiguration.KCS_WS_URI_DEFAULT;
import static org.kurento.test.TestConfiguration.KCS_WS_URI_PROP;
import static org.kurento.test.TestConfiguration.KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_AUTOSTART_PROP;
import static org.kurento.test.TestConfiguration.KMS_HTTP_PORT_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_HTTP_PORT_PROP;
import static org.kurento.test.TestConfiguration.KMS_PRINT_LOG_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_PRINT_LOG_PROP;
import static org.kurento.test.TestConfiguration.KMS_RABBITMQ_ADDRESS_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_RABBITMQ_ADDRESS_PROP;
import static org.kurento.test.TestConfiguration.KMS_TRANSPORT_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_TRANSPORT_PROP;
import static org.kurento.test.TestConfiguration.KMS_TRANSPORT_RABBITMQ_VALUE;
import static org.kurento.test.TestConfiguration.KMS_TRANSPORT_WS_VALUE;
import static org.kurento.test.TestConfiguration.KMS_WS_URI_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_WS_URI_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_TESTFILES_DEFAULT;
import static org.kurento.test.TestConfiguration.KURENTO_TESTFILES_PROP;
import static org.kurento.test.TestConfiguration.PROJECT_PATH_DEFAULT;
import static org.kurento.test.TestConfiguration.PROJECT_PATH_PROP;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.commons.Address;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.test.Shell;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class KurentoServicesTestHelper {

	// Attributes
	private static KurentoMediaServerManager kms;
	private static KurentoControlServerManager kcs;

	private static String testCaseName;
	private static String testName;
	private static String testDir;
	private static String kmsAutostart = KMS_AUTOSTART_DEFAULT;
	private static String kcsAutostart = KMS_AUTOSTART_DEFAULT;
	private static String kmsPrintLog;
	private static List<File> logFiles;
	private static ConfigurableApplicationContext appContext;

	public static void startKurentoServicesIfNeccessary() throws IOException {

		startKurentoMediaServerIfNecessary();
		startKurentoControlServerIfNecessary();
	}

	private static void startKurentoControlServerIfNecessary() {

		kcsAutostart = getProperty(KCS_AUTOSTART_PROP, KCS_AUTOSTART_DEFAULT);

		switch (kcsAutostart) {
		case AUTOSTART_FALSE_VALUE:
			break;
		case AUTOSTART_TEST_VALUE:
			startKurentoControlServer();
			break;
		case AUTOSTART_TEST_SUITE_VALUE:
			if (kcs == null) {
				startKurentoControlServer();
			}
			break;
		default:
			throw new IllegalArgumentException("The value '" + kcsAutostart
					+ "' is not valid for property " + KCS_AUTOSTART_PROP);
		}
	}

	private static void startKurentoMediaServerIfNecessary()
			throws IOException {
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
			if (kms == null) {
				startKurentoMediaServer();
			}
			break;
		default:
			throw new IllegalArgumentException("The value '" + kmsAutostart
					+ "' is not valid for property " + KMS_AUTOSTART_PROP);
		}
	}

	public static KurentoMediaServerManager startKurentoMediaServer()
			throws IOException {

		String transport = PropertiesManager.getProperty(KMS_TRANSPORT_PROP,
				KMS_TRANSPORT_DEFAULT);

		int httpPort = getKmsHttpPort();

		switch (transport) {
		case KMS_TRANSPORT_WS_VALUE:

			kms = KurentoMediaServerManager.createWithWsTransport(getWsUri(),
					httpPort);
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
		kms.setTestMethodName(getSimpleTestName());
		kms.setTestDir(testDir);
		kms.start();

		return kms;
	}

	public static KurentoControlServerManager startKurentoControlServer() {
		return startKurentoControlServer(
				getProperty(KCS_WS_URI_PROP, KCS_WS_URI_DEFAULT));
	}

	public static KurentoControlServerManager startKurentoControlServer(
			String wsUriProp) {

		JsonRpcClient client = KurentoClientTestFactory
				.createJsonRpcClient("kcs");

		try {

			URI wsUri = new URI(wsUriProp);
			int port = wsUri.getPort();
			String path = wsUri.getPath();
			kcs = new KurentoControlServerManager(client, port, path);

			return kcs;

		} catch (URISyntaxException e) {
			throw new KurentoException(
					KCS_WS_URI_PROP + " invalid format: " + wsUriProp);
		}
	}

	public static ConfigurableApplicationContext startHttpServer(
			Object... sources) {
		appContext = new SpringApplication(sources)
				.run("--server.port=" + getAppHttpPort());
		return appContext;
	}

	public static void teardownServices() throws IOException {
		teardownHttpServer();
		teardownKurentoMediaServer();
		teardownKurentoControlServer();
	}

	public static void teardownHttpServer() {
		if (appContext != null) {
			appContext.stop();
			appContext.close();
		}
	}

	public static void teardownKurentoControlServer() {
		if (kcs != null && kcsAutostart.equals(AUTOSTART_TEST_VALUE)) {
			kcs.destroy();
			kcs = null;
		}
	}

	public static void teardownKurentoMediaServer() throws IOException {
		if (kms != null && kmsAutostart.equals(AUTOSTART_TEST_VALUE)) {
			kms.destroy();
			kms = null;
		}
	}

	public static void setTestName(String testName) {
		KurentoServicesTestHelper.testName = testName;
	}

	public static String getTestName() {
		return KurentoServicesTestHelper.testName;
	}

	public static String getSimpleTestName() {
		String out = testName;
		if (testName != null && out.indexOf(":") != -1) {
			// This happens in performance tests with data from JUnit parameters
			out = out.substring(0, out.indexOf(":")) + "]";
		}
		return out;
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

	public static String getBowerKurentoClientTag() {
		return PropertiesManager.getProperty(BOWER_KURENTO_CLIENT_TAG_PROP,
				BOWER_KURENTO_CLIENT_TAG_DEFAULT);
	}

	public static String getBowerKurentoUtilsTag() {
		return PropertiesManager.getProperty(BOWER_KURENTO_UTILS_TAG_PROP,
				BOWER_KURENTO_UTILS_TAG_DEFAULT);
	}

	public static String getTestFilesPath() {
		return PropertiesManager.getProperty(KURENTO_TESTFILES_PROP,
				KURENTO_TESTFILES_DEFAULT);
	}

	public static Address getRabbitMqAddress() {
		return getRabbitMqAddress(null);
	}

	public static Address getRabbitMqAddress(String prefix) {
		return PropertiesManager.getProperty(prefix, KMS_RABBITMQ_ADDRESS_PROP,
				KMS_RABBITMQ_ADDRESS_DEFAULT);
	}

	public static String getWsUri() {
		return getWsUri(null);
	}

	public static String getWsUri(String prefix) {
		return PropertiesManager.getProperty(prefix, KMS_WS_URI_PROP,
				KMS_WS_URI_DEFAULT);
	}

	public static void addServerLogFilePath(File logFile) {
		if (logFiles == null) {
			logFiles = new ArrayList<>();
		}
		logFiles.add(logFile);
	}

	public static List<File> getServerLogFiles() {
		return logFiles;
	}

	private static void createFolder(String folder) {
		File folderFile = new File(folder);
		if (!folderFile.exists()) {
			folderFile.mkdirs();
		}
		Shell.runAndWait("chmod", "a+w", folder);
	}

}
