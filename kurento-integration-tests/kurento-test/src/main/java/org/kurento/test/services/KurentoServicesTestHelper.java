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
import static org.kurento.test.config.TestConfiguration.APP_HTTP_PORT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.APP_HTTP_PORT_PROP;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_FALSE_VALUE;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_TESTSUITE_VALUE;
import static org.kurento.test.config.TestConfiguration.AUTOSTART_TEST_VALUE;
import static org.kurento.test.config.TestConfiguration.BOWER_KURENTO_CLIENT_TAG_DEFAULT;
import static org.kurento.test.config.TestConfiguration.BOWER_KURENTO_CLIENT_TAG_PROP;
import static org.kurento.test.config.TestConfiguration.BOWER_KURENTO_UTILS_TAG_DEFAULT;
import static org.kurento.test.config.TestConfiguration.BOWER_KURENTO_UTILS_TAG_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_AUTOSTART_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_AUTOSTART_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_HTTP_PORT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_HTTP_PORT_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_RABBITMQ_ADDRESS_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_RABBITMQ_ADDRESS_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_DOCKER;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_TRANSPORT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_TRANSPORT_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_TRANSPORT_RABBITMQ_VALUE;
import static org.kurento.test.config.TestConfiguration.KMS_TRANSPORT_WS_VALUE;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_PROP;
import static org.kurento.test.config.TestConfiguration.KURENTO_TESTFILES_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KURENTO_TESTFILES_PROP;

import java.io.IOException;

import org.kurento.commons.Address;
import org.kurento.commons.PropertiesManager;
import org.kurento.test.docker.Docker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class KurentoServicesTestHelper {

	public static Logger log = LoggerFactory
	        .getLogger(KurentoServicesTestHelper.class);

	// Attributes
	private static KurentoMediaServerManager kms;
	private static KurentoMediaServerManager fakeKms;
	private static String kmsAutostart = KMS_AUTOSTART_DEFAULT;
	private static String fakeKmsAutostart = KMS_AUTOSTART_DEFAULT;
	private static ConfigurableApplicationContext appContext;

	public static void startKurentoServicesIfNeccessary() throws IOException {
		startKurentoMediaServerIfNecessary();
	}

	private static void startKurentoMediaServerIfNecessary()
	        throws IOException {
		kmsAutostart = getProperty(KMS_AUTOSTART_PROP, KMS_AUTOSTART_DEFAULT);
		fakeKmsAutostart = getProperty(FAKE_KMS_AUTOSTART_PROP,
		        FAKE_KMS_AUTOSTART_DEFAULT);

		startKms(kmsAutostart, false);
		startKms(fakeKmsAutostart, true);

	}

	private static void startKms(String kmsAutostart, boolean isFake)
	        throws IOException {

		KurentoMediaServerManager kmsToBeStarted = isFake ? fakeKms : kms;

		switch (kmsAutostart) {
		case AUTOSTART_FALSE_VALUE:
			if (kms == null) {
				kms = alreadyStartedKurentoMediaServer();
			}

			break;
		case AUTOSTART_TEST_VALUE:
			startKurentoMediaServer(isFake);
			break;
		case AUTOSTART_TESTSUITE_VALUE:
			if (kmsToBeStarted == null) {
				startKurentoMediaServer(isFake);
			}
			break;
		default:
			throw new IllegalArgumentException("The value '" + kmsAutostart
			        + "' is not valid for property "
			        + (isFake ? FAKE_KMS_AUTOSTART_PROP : KMS_AUTOSTART_PROP));
		}
	}

	public static KurentoMediaServerManager alreadyStartedKurentoMediaServer()
	        throws IOException {

		return KurentoMediaServerManager.kmsAlreadyStarted(getWsUri());
	}

	public static KurentoMediaServerManager startKurentoMediaServer(
	        boolean isFake) throws IOException {

		KurentoMediaServerManager kmsToBeStarted = isFake ? fakeKms : kms;

		String transport = PropertiesManager.getProperty(KMS_TRANSPORT_PROP,
		        KMS_TRANSPORT_DEFAULT);

		int httpPort = getKmsHttpPort();

		switch (transport) {
		case KMS_TRANSPORT_WS_VALUE:

			kmsToBeStarted = KurentoMediaServerManager
			        .createWithWsTransport(getWsUri(), httpPort);
			break;
		case KMS_TRANSPORT_RABBITMQ_VALUE:

			kmsToBeStarted = KurentoMediaServerManager
			        .createWithRabbitMqTransport(getRabbitMqAddress(),
			                httpPort);
			break;

		default:
			throw new IllegalArgumentException("The value " + transport
			        + " is not valid for property " + KMS_TRANSPORT_PROP);
		}

		boolean docker = KMS_SCOPE_DOCKER.equals(PropertiesManager
		        .getProperty(KMS_SCOPE_PROP, KMS_SCOPE_DEFAULT));

		kmsToBeStarted.setDocker(docker);

		if (docker) {
			Docker dockerClient = Docker.getSingleton();
			if (dockerClient.isRunningInContainer()) {
				kmsToBeStarted.setDockerContainerName(
				        dockerClient.getContainerName() + "_kms");
			}
		}
		kmsToBeStarted.start(isFake);

		if (isFake) {
			fakeKms = kmsToBeStarted;
		} else {
			kms = kmsToBeStarted;
		}

		return kmsToBeStarted;

	}

	public static ConfigurableApplicationContext startHttpServer(
	        Object... sources) {

		System.setProperty("java.security.egd", "file:/dev/./urandom");

		appContext = new SpringApplication(sources)
		        .run("--server.port=" + getAppHttpPort());
		return appContext;
	}

	public static void teardownServices() throws IOException {
		teardownHttpServer();
		teardownKurentoMediaServer();
	}

	public static void teardownHttpServer() {
		if (appContext != null) {
			appContext.stop();
			appContext.close();
		}
	}

	public static void teardownKurentoMediaServer() throws IOException {
		log.debug("Teardown KMS: kms={} kmsAutostart={}", kms, kmsAutostart);
		if (kms != null) {
			kms.retrieveLogs();

			if (kmsAutostart.equals(AUTOSTART_TEST_VALUE)) {
				kms.destroy();
				kms = null;
			}
		}
		if (fakeKms != null && fakeKmsAutostart.equals(AUTOSTART_TEST_VALUE)) {
			fakeKms.destroy();
			fakeKms = null;
		}
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

}
