/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test;

import org.kurento.commons.Address;

/**
 * Test properties.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.0.0
 */
public class TestConfiguration {

	// Host (address, port, protocol, path)
	public static final String TEST_HOST_PROPERTY = "test.host";
	public static final String TEST_PUBLIC_IP_PROPERTY = "test.public.ip";
	public static final String TEST_PUBLIC_IP_DEFAULT = "127.0.0.1";

	public static final String TEST_PORT_PROPERTY = "test.port";
	public static final String TEST_PUBLIC_PORT_PROPERTY = "test.public.port";
	public static final String APP_HTTP_PORT_PROP = "server.port";
	public static final int APP_HTTP_PORT_DEFAULT = 7779;

	public static final String TEST_PATH_PROPERTY = "test.path";
	public static final String TEST_PATH_DEFAULT = "/";

	public static final String TEST_PROTOCOL_PROPERTY = "test.protocol";
	public static final String TEST_PROTOCOL_DEFAULT = "http";

	public final static String TEST_URL_TIMEOUT_PROPERTY = "test.url.timeout";
	public final static int TEST_URL_TIMEOUT_DEFAULT = 30; // seconds

	public final static String TEST_CONFIG_JSON_DEFAULT = "test.conf.json";
	public final static String TEST_CONFIG_EXECUTIONS_PROPERTY = "test.config.executions";
	public final static String TEST_CONFIG_EXECUTIONS_DEFAULT = "executions";

	// Saucelabs
	public static final String SAUCELAB_USER_PROPERTY = "saucelab.user";
	public static final String SAUCELAB_KEY_PROPERTY = "saucelab.key";
	public static final String SAUCELAB_IDLE_TIMEOUT_PROPERTY = "saucelab.idle.timeout";
	public static final int SAUCELAB_IDLE_TIMEOUT_DEFAULT = 120; // seconds
	public static final String SAUCELAB_COMMAND_TIMEOUT_PROPERTY = "saucelab.command.timeout";
	public static final int SAUCELAB_COMMAND_TIMEOUT_DEFAULT = 300; // seconds
	public static final int SAUCELAB_COMMAND_TIMEOUT_MAX = 600; // seconds
	public static final String SAUCELAB_MAX_DURATION_PROPERTY = "saucelab.max.duration";
	public static final int SAUCELAB_MAX_DURATION_DEFAULT = 1800; // seconds

	// Selenium
	public static final String SELENIUM_VERSION = "selenium.version";
	public static final String SELENIUM_HUB_ADDRESS = "selenium.hub.address";
	public static final String SELENIUM_HUB_ADDRESS_DEFAULT = "127.0.0.1";

	public static final String SELENIUM_HUB_PORT_PROPERTY = "selenium.hub.port";
	public static final int SELENIUM_HUB_PORT_DEFAULT = 4444;

	public static final String SELENIUM_REMOTEWEBDRIVER_TIME_PROPERTY = "selenium.remotedriver.timeout";
	public static final int SELENIUM_REMOTEWEBDRIVER_TIME_DEFAULT = 120; // seconds

	public static final String SELENIUM_NODES_LIST_PROPERTY = "test.nodes.list";
	public static final String SELENIUM_NODES_LIST_DEFAULT = "node-list.txt";
	public static final String SELENIUM_NODES_FILE_LIST_PROPERTY = "test.nodes.file.list";
	public static final String SELENIUM_NODES_URL_PROPERTY = "test.nodes.url.list";

	public static final String TEST_NODE_LOGIN_PROPERTY = "test.node.login";
	public static final String TEST_NODE_PASSWD_PROPERTY = "test.node.passwd";
	public static final String TEST_NODE_PEM_PROPERTY = "test.node.pem";

	public static final String TEST_SCREEN_SHARE_TITLE_PROPERTY = "test.screenshare.title";
	public static final String TEST_SCREEN_SHARE_TITLE_DEFAULT = "Entire screen";
	public static final String TEST_SCREEN_SHARE_TITLE_DEFAULT_WIN = "Screen 1";

	public static final String SELENIUM_MAX_DRIVER_ERROR_PROPERTY = "selenium.max.driver.error";
	public static final int SELENIUM_MAX_DRIVER_ERROR_DEFAULT = 10;

	public static final String SELENIUM_REMOTE_HUB_URL_PROPERTY = "selenium.remote.hub.url";

	public static final String SELENIUM_SCOPE_PROPERTY = "test.selenium.scope";

	public static final String SELENIUM_RECORD_PROPERTY = "test.selenium.record";
	public static final boolean SELENIUM_RECORD_DEFAULT = true;

	// Docker
	public static final String DOCKER_HUB_IMAGE_PROPERTY = "docker.hub.image";
	public static final String DOCKER_HUB_IMAGE_DEFAULT = "selenium/hub:2.48.2";

	public static final String DOCKER_VNCRECORDER_IMAGE_PROPERTY = "docker.vncrecorder.image";
	public static final String DOCKER_VNCRECORDER_IMAGE_DEFAULT = "softsam/vncrecorder";

	public static final String DOCKER_NODE_CHROME_IMAGE_PROPERTY = "docker.node.chrome.image";
	public static final String DOCKER_NODE_CHROME_IMAGE_DEFAULT = "selenium/node-chrome:2.48.2";

	public static final String DOCKER_NODE_FIREFOX_IMAGE_PROPERTY = "docker.node.firefox.image";
	public static final String DOCKER_NODE_FIREFOX_IMAGE_DEFAULT = "selenium/node-firefox:2.48.2";

	public static final String DOCKER_NODE_CHROME_DEBUG_IMAGE_PROPERTY = "docker.node.chrome-debug.image";
	public static final String DOCKER_NODE_CHROME_DEBUG_IMAGE_DEFAULT = "selenium/node-chrome-debug:2.48.1";

	public static final String DOCKER_NODE_FIREFOX_DEBUG_IMAGE_PROPERTY = "docker.node.firefox-debug.image";
	public static final String DOCKER_NODE_FIREFOX_DEBUG_IMAGE_DEFAULT = "selenium/node-firefox-debug:2.48.2";

	public static final String DOCKER_HUB_CONTAINER_NAME_PROPERTY = "docker.hub.container.name";
	public static final String DOCKER_HUB_CONTAINER_NAME_DEFAULT = "hub";

	public static final String DOCKER_VNCRECORDER_CONTAINER_NAME_PROPERTY = "docker.vncrecorder.container.name";
	public static final String DOCKER_VNCRECORDER_CONTAINER_NAME_DEFAULT = "vncrecorder";

	// Parallel browsers
	public static final String CLIENT_RATE_PROPERTY = "parallel.browsers.rate";
	public static final int CLIENT_RATE_DEFAULT = 5000; // milliseconds

	public static final String HOLD_TIME_PROPERTY = "parallel.browsers.holdtime";
	public static final int HOLD_TIME_DEFAULT = 10000; // milliseconds

	// Monitor
	public static final String DEFAULT_MONITOR_RATE_PROPERTY = "test.monitor.rate";
	public static final int DEFAULT_MONITOR_RATE_DEFAULT = 1000; // milliseconds

	// KMS
	public static final String KMS_WS_URI_PROP = "kms.ws.uri";
	public static final String KMS_WS_URI_DEFAULT = "ws://localhost:8888/kurento";

	public static final String KMS_LOG_PATH_PROP = "kms.log.path";
	public static final String KMS_LOG_PATH_DEFAULT = "/var/log/kurento-media-server/";

	public static final String KURENTO_WORKSPACE_PROP = "test.workspace";
	public static final String KURENTO_WORKSPACE_DEFAULT = "/tmp";

	public static final String KURENTO_WORKSPACE_HOST_PROP = "test.workspace.host";
	public static final String KURENTO_WORKSPACE_HOST_DEFAULT = "/tmp";

	public static final String KURENTO_GST_PLUGINS_PROP = "kms.gst.plugins";
	public static final String KURENTO_GST_PLUGINS_DEFAULT = "";

	public static final String KURENTO_SERVER_COMMAND_PROP = "kms.command";
	public static final String KURENTO_SERVER_COMMAND_DEFAULT = "/usr/bin/kurento-media-server";

	public static final String KURENTO_SERVER_DEBUG_PROP = "kms.debug";
	public static final String KURENTO_SERVER_DEBUG_DEFAULT = "2,*media_server*:5,*Kurento*:5,KurentoMediaServerServiceHandler:7";

	public static final String KURENTO_KMS_LOGIN_PROP = "kms.login";
	public static final String KURENTO_KMS_PASSWD_PROP = "kms.passwd";
	public static final String KURENTO_KMS_PEM_PROP = "kms.pem";

	public static final String KMS_TRANSPORT_PROP = "kms.transport";
	public static final String KMS_TRANSPORT_WS_VALUE = "ws";
	public static final String KMS_TRANSPORT_RABBITMQ_VALUE = "rabbitmq";
	public static final String KMS_TRANSPORT_DEFAULT = KMS_TRANSPORT_WS_VALUE;

	public static final String KMS_DOCKER_IMAGE_NAME_PROP = "test.kms.docker.image.name";
	public static final String KMS_DOCKER_IMAGE_NAME_DEFAULT = "kurento/kurento-media-server-dev:latest";

	public static final String KMS_DOCKER_IMAGE_FORCE_PULLING_PROP = "test.kms.docker.image.forcepulling";
	public static final boolean KMS_DOCKER_IMAGE_FORCE_PULLING_DEFAULT = true;

	public static final String KMS_PRINT_LOG_PROP = "kms.print.log";
	public static final String KMS_PRINT_LOG_DEFAULT = "true";

	public static final String KMS_HTTP_PORT_PROP = "kms.http.port";
	public static final int KMS_HTTP_PORT_DEFAULT = 9091;

	public static final String KMS_RABBITMQ_ADDRESS_PROP = "kms.rabbitmq.address";
	public static final Address KMS_RABBITMQ_ADDRESS_DEFAULT = new Address(
			"127.0.0.1", 5672);

	// Binary test files
	public static final String KURENTO_TESTFILES_PROP = "test.files";
	public static final String KURENTO_TESTFILES_DEFAULT = "/var/lib/jenkins/test-files";

	// KCS properties
	public static final String KCS_WS_URI_PROP = "kcs.ws.uri";
	public static final String KCS_WS_URI_DEFAULT = "ws://localhost:8889/kurento";

	// Project
	public static final String PROJECT_PATH_PROP = "project.path";
	public static final String PROJECT_PATH_DEFAULT = ".";

	// Autostart
	public static final String AUTOSTART_FALSE_VALUE = "false";
	public static final String AUTOSTART_TEST_VALUE = "test";
	public static final String AUTOSTART_TESTSUITE_VALUE = "testsuite";

	public static final String KMS_AUTOSTART_PROP = "test.kms.autostart";
	public static final String KMS_AUTOSTART_DEFAULT = AUTOSTART_TEST_VALUE;

	public final static String TEST_APP_AUTOSTART_PROPERTY = "test.app.autostart";
	public final static String TEST_APP_AUTOSTART_DEFAULT = AUTOSTART_TESTSUITE_VALUE;

	public static final String KMS_SCOPE_PROP = "test.kms.scope";
	public static final String KMS_SCOPE_LOCAL = "local";
	public static final String KMS_SCOPE_DOCKER = "docker";
	public static final String KMS_SCOPE_DEFAULT = KMS_SCOPE_LOCAL;

	public static final String KCS_AUTOSTART_PROP = "test.kcs.autostart";
	public static final String KCS_AUTOSTART_DEFAULT = AUTOSTART_FALSE_VALUE;

	// Bower
	public static final String BOWER_KURENTO_CLIENT_TAG_PROP = "bower.kurentoclient.tag";
	public static final String BOWER_KURENTO_CLIENT_TAG_DEFAULT = "";
	public static final String BOWER_KURENTO_UTILS_TAG_PROP = "bower.kurentoutils.tag";
	public static final String BOWER_KURENTO_UTILS_TAG_DEFAULT = "";

	// Fake KMS (extra clients typically in performance/load/stability tests)
	public static final String FAKE_KMS_WS_URI_PROP = "fake.kms.ws.uri";
	public static final String FAKE_KMS_LOGIN_PROP = "fake.kms.login";
	public static final String FAKE_KMS_PASSWD_PROP = "fake.kms.passwd";
	public static final String FAKE_KMS_PEM_PROP = "fake.kms.pem";
	public static final String FAKE_KMS_AUTOSTART_PROP = "fake.kms.autostart";
	public static final String FAKE_KMS_AUTOSTART_DEFAULT = AUTOSTART_FALSE_VALUE;

	// Retries
	public static final String TEST_NUMRETRIES_PROPERTY = "test.num.retries";
	public static final int TEST_NUM_NUMRETRIES_DEFAULT = 1;

	// Test report
	public static final String TEST_REPORT_PROPERTY = "test.report";
	public static final String TEST_REPORT_DEFAULT = "target/report.html";

}
