/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.test.config;

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
  public static final String APP_HTTPS_PORT_PROP = "server.port.https";
  public static final int APP_HTTPS_PORT_DEFAULT = 8443;
  public static final String APP_HTTP_PORT_PROP = "server.port.http";
  public static final int APP_HTTP_PORT_DEFAULT = 8090;

  public static final String TEST_PATH_PROPERTY = "test.path";
  public static final String TEST_PATH_DEFAULT = "/";

  public static final String TEST_PROTOCOL_PROPERTY = "test.protocol";
  public static final String TEST_PROTOCOL_DEFAULT = "https";

  public static final String TEST_URL_TIMEOUT_PROPERTY = "test.url.timeout";
  public static final int TEST_URL_TIMEOUT_DEFAULT = 30; // seconds

  public static final String TEST_DURATION_PROPERTY = "test.duration";

  public static final String TEST_CONFIG_FILE_DEFAULT = "test.config.file";
  public static final String TEST_CONFIG_JSON_DEFAULT = "test.conf.json";
  public static final String TEST_CONFIG_EXECUTIONS_PROPERTY = "test.config.executions";
  public static final String TEST_CONFIG_EXECUTIONS_DEFAULT = "executions";

  public static final String TEST_ICE_SERVER_URL_PROPERTY = "test.ice.server.url";
  public static final String TEST_ICE_SERVER_USERNAME_PROPERTY = "test.ice.server.username";
  public static final String TEST_ICE_SERVER_CREDENTIAL_PROPERTY = "test.ice.server.credential";

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

  public static final String SELENIUM_REMOTEWEBDRIVER_TIME_PROPERTY =
      "selenium.remotedriver.timeout";
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
  public static final int SELENIUM_MAX_DRIVER_ERROR_DEFAULT = 15;

  public static final String SELENIUM_REMOTE_HUB_URL_PROPERTY = "selenium.remote.hub.url";

  public static final String SELENIUM_SCOPE_PROPERTY = "test.selenium.scope";

  // TODO review recording
  public static final String SELENIUM_RECORD_PROPERTY = "test.selenium.record";
  public static final boolean SELENIUM_RECORD_DEFAULT = true;

  public static final String TEST_ICE_CANDIDATE_SELENIUM_TYPE = "test.ice.candidate.selenium.type";
  public static final String TEST_SELENIUM_DNAT = "test.selenium.dnat";
  public static final boolean TEST_SELENIUM_DNAT_DEFAULT = false;
  public static final String TEST_SELENIUM_TRANSPORT = "test.selenium.transport";

  public static final String DOCKER_NODE_CHROME_IMAGE_PROPERTY = "docker.node.chrome.image";
  public static final String DOCKER_NODE_CHROME_IMAGE_DEFAULT = "elastestbrowsers/chrome:latest";

  public static final String DOCKER_NODE_FIREFOX_IMAGE_PROPERTY = "docker.node.firefox.image";
  public static final String DOCKER_NODE_FIREFOX_IMAGE_DEFAULT = "elastestbrowsers/firefox:latest";

  public static final String TEST_POST_CONTAINER_RUN_URL = "test.post.container.run.url";

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
  public static final String KMS_WS_URI_PROP_EXPORT = "kms.url";
  public static final String KMS_WS_URI_DEFAULT = "ws://localhost:8888/kurento";

  public static final String KMS_LOG_PATH_PROP = "kms.log.path";
  public static final String KMS_LOG_PATH_DEFAULT = "/var/log/kurento-media-server/";

  public static final String KSM_GST_PLUGINS_PROP = "kms.gst.plugins";
  public static final String KMS_GST_PLUGINS_DEFAULT = "";

  public static final String KMS_SERVER_COMMAND_PROP = "kms.command";
  public static final String KMS_SERVER_COMMAND_DEFAULT = "/usr/bin/kurento-media-server";

  public static final String KMS_SERVER_DEBUG_PROP = "kms.debug";
  public static final String KMS_SERVER_DEBUG_DEFAULT =
      "2,*media_server*:5,*Kurento*:5,KurentoMediaServerServiceHandler:7";

  public static final String KMS_LOGIN_PROP = "kms.login";
  public static final String KMS_PASSWD_PROP = "kms.passwd";
  public static final String KMS_PEM_PROP = "kms.pem";

  public static final String KMS_DOCKER_IMAGE_NAME_PROP = "test.kms.docker.image.name";
  public static final String KMS_DOCKER_IMAGE_NAME_DEFAULT =
      "kurento/kurento-media-server:dev";

  public static final String KMS_DOCKER_IMAGE_FORCE_PULLING_PROP =
      "test.kms.docker.image.forcepulling";
  public static final boolean KMS_DOCKER_IMAGE_FORCE_PULLING_DEFAULT = true;

  public static final String KMS_STUN_IP_PROPERTY = "kms.stun.ip";
  public static final String KMS_STUN_PORT_PROPERTY = "kms.stun.port";

  public static final String TEST_ICE_CANDIDATE_KMS_TYPE = "test.ice.candidate.kms.type";
  public static final String TEST_KMS_DNAT = "test.kms.dnat";
  public static final boolean TEST_KMS_DNAT_DEFAULT = false;
  public static final String TEST_KMS_TRANSPORT = "test.kms.transport";

  public static final String KMS_GENERATE_RTP_PTS_STATS_PROPERTY = "kms.generate.rtp.pts.stats";

  // S3 properties
  public static final String KMS_DOCKER_S3_BUCKET_NAME = "s3.bucket.name";
  public static final String KMS_DOCKER_S3_ACCESS_KEY_ID = "s3.access.key.id";
  public static final String KMS_DOCKER_S3_SECRET_ACCESS_KEY = "s3.secret.access.key";
  public static final String KMS_DOCKER_S3_HOSTNAME = "s3.hostname";

  public static final String KMS_HTTP_PORT_PROP = "kms.http.port";
  public static final int KMS_HTTP_PORT_DEFAULT = 9091;

  // Autostart
  public static final String AUTOSTART_FALSE_VALUE = "false";
  public static final String AUTOSTART_TEST_VALUE = "test";
  public static final String AUTOSTART_TESTCLASS_VALUE = "testclass";
  public static final String AUTOSTART_TESTSUITE_VALUE = "testsuite";

  public static final String KMS_AUTOSTART_PROP = "test.kms.autostart";
  public static final String KMS_AUTOSTART_DEFAULT = AUTOSTART_TEST_VALUE;

  public static final String TEST_APP_AUTOSTART_PROPERTY = "test.app.autostart";
  public static final String TEST_APP_AUTOSTART_DEFAULT = AUTOSTART_TESTSUITE_VALUE;

  public static final String KMS_SCOPE_PROP = "test.kms.scope";
  public static final String KMS_SCOPE_LOCAL = "local";
  public static final String KMS_SCOPE_DOCKER = "docker";
  public static final String KMS_SCOPE_ELASTEST = "elastest";
  public static final String KMS_SCOPE_DEFAULT = KMS_SCOPE_LOCAL;

  // Fake KMS
  public static final String FAKE_KMS_WS_URI_PROP = "fake.kms.ws.uri";
  public static final String FAKE_KMS_WS_URI_DEFAULT = KMS_WS_URI_DEFAULT;
  public static final String FAKE_KMS_WS_URI_PROP_EXPORT = "fake.kms.url";
  public static final String FAKE_KMS_LOGIN_PROP = "fake.kms.login";
  public static final String FAKE_KMS_PASSWD_PROP = "fake.kms.passwd";
  public static final String FAKE_KMS_PEM_PROP = "fake.kms.pem";
  public static final String FAKE_KMS_AUTOSTART_PROP = "fake.kms.autostart";
  public static final String FAKE_KMS_AUTOSTART_DEFAULT = AUTOSTART_FALSE_VALUE;
  public static final String FAKE_KMS_SCOPE_PROP = "fake.kms.scope";
  public static final String FAKE_KMS_SCOPE_DEFAULT = KMS_SCOPE_LOCAL;

  // Bower
  public static final String BOWER_KURENTO_CLIENT_TAG_PROP = "bower.kurentoclient.tag";
  public static final String BOWER_KURENTO_CLIENT_TAG_DEFAULT = "";
  public static final String BOWER_KURENTO_UTILS_TAG_PROP = "bower.kurentoutils.tag";
  public static final String BOWER_KURENTO_UTILS_TAG_DEFAULT = "";

  // Test services
  public static final String TEST_NUMRETRIES_PROPERTY = "test.num.retries";
  public static final int TEST_NUM_NUMRETRIES_DEFAULT = 1;

  public static final String TEST_REPORT_PROPERTY = "test.report";
  public static final String TEST_REPORT_DEFAULT = "target/report.html";

  public static final String TEST_PRINT_LOG_PROP = "test.print.log";
  public static final boolean TEST_PRINT_LOG_DEFAULT = true;

  public static final String TEST_FILES_URL_PROP = "test.files.url";
  public static final String TEST_RECORD_URL_PROP = "test.record.url";
  public static final String TEST_RECORD_DEFAULTPATH_PROP = "test.record.defaultpath";

  // FIXME: When CI can, remove TEST_FILES_DISK_PROP_OLD
  public static final String TEST_FILES_DISK_PROP_OLD = "test.files";
  public static final String TEST_FILES_DISK_PROP = "test.files.disk";
  public static final String TEST_FILES_DISK_DEFAULT = "/var/lib/jenkins/test-files";

  // FIXME: When CI can, remove TEST_FILES_S3_PROP_OLD
  public static final String TEST_FILES_S3_PROP_OLD = "test.s3";
  public static final String TEST_FILES_S3_PROP = "test.files.s3";
  public static final String TEST_FILES_S3_DEFAULT = "kurento-s3-test";

  public static final String TEST_FILES_HTTP_PROP = "test.files.http";
  public static final String TEST_FILES_HTTP_DEFAULT = "files.openvidu.io";

  public static final String TEST_FILES_MONGO_PROP = "test.files.mongodb";
  public static final String TEST_FILES_MONGO_DEFAULT = "files.openvidu.io:27017";

  public static final String TEST_PROJECT_PATH_PROP = "test.project.path";
  public static final String TEST_PROJECT_PATH_DEFAULT = "target/surefire-reports/";

  public static final String TEST_WORKSPACE_PROP = "test.workspace";
  public static final String TEST_WORKSPACE_DEFAULT = "/tmp";

  public static final String TEST_WORKSPACE_HOST_PROP = "test.workspace.host";
  public static final String TEST_WORKSPACE_HOST_DEFAULT = "/tmp";

  // Other keys
  public static final String TEST_SEEK_REPETITIONS = "test.seek.repetitions";
  public static final int TEST_SEEK_REPETITIONS_DEFAULT = 20;

}
