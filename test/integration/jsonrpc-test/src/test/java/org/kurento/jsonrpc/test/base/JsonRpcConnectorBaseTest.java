/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc.test.base;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.JsonRpcConnectorTests;
import org.kurento.jsonrpc.client.AbstractJsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientHttp;
import org.kurento.jsonrpc.client.JsonRpcClientNettyWebSocket;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Category(JsonRpcConnectorTests.class)
public class JsonRpcConnectorBaseTest {
  private static final Logger log = LoggerFactory.getLogger(JsonRpcConnectorBaseTest.class);

  protected static final int MAX_WS_CONNECTIONS = 50;
  protected static ConfigurableApplicationContext server;

  @BeforeClass
  public static void startServer() throws Exception {

    if (server == null || !server.isActive()) {

      System.setProperty("ws.maxSessions", Integer.toString(MAX_WS_CONNECTIONS));
      System.setProperty("java.security.egd", "file:/dev/./urandom");

      Properties properties = new Properties();
      properties.put("server.port", getPort());

      SpringApplication application = new SpringApplication(BootTestApplication.class);

      application.setDefaultProperties(properties);

      log.debug("Properties: {}", properties);

      server = application.run();

    }
  }

  @AfterClass
  public static void stopServer() {

    if (server != null) {
      server.close();
      server = null;
    }
  }

  protected static String getPort() {
    String port = System.getProperty("http.port");
    if (port == null) {
      port = "7788";
    }
    return port;
  }

  protected JsonRpcClient createJsonRpcClient(String servicePath) {
    return createJsonRpcClient(servicePath, null);
  }

  protected JsonRpcClient createJsonRpcClient(String servicePath,
      JsonRpcWSConnectionListener listener) {

    String clientType = System.getProperty("jsonrpcconnector-client-type");

    if (clientType == null) {
      clientType = "ws";
    }

    JsonRpcClient client;
    if ("ws".equals(clientType)) {
      client = new JsonRpcClientWebSocket("ws://localhost:" + getPort() + servicePath, listener);
    } else if ("netty".equals(clientType)) {
      client =
          new JsonRpcClientNettyWebSocket("ws://localhost:" + getPort() + servicePath, listener);
    } else if ("http".equals(clientType)) {
      client = new JsonRpcClientHttp("http://localhost:" + getPort() + servicePath);
    } else {
      throw new RuntimeException(
          "Unrecognized property value jsonrpcconnector-client-type=" + clientType);
    }

    return client;
  }

  protected AbstractJsonRpcClientWebSocket createJsonRpcClientWebSocket(String servicePath,
      JsonRpcWSConnectionListener listener) {

    String clientType = System.getProperty("jsonrpcconnector-client-type");

    if (clientType == null) {
      clientType = "ws";
    }

    String url = "ws://localhost:" + getPort() + servicePath;

    log.debug("Creating JsonRpcClient type={}, url={}", clientType, url);
    AbstractJsonRpcClientWebSocket client;
    if ("ws".equals(clientType)) {
      client = new JsonRpcClientWebSocket(url, listener);
    } else if ("netty".equals(clientType)) {
      client =
          new JsonRpcClientNettyWebSocket(url, listener);
    } else {
      throw new RuntimeException("Unrecognized property value jsonrpcconnector-client-type="
          + clientType + ", has to be ws or netty");
    }

    return client;
  }

  public static void main(String[] args) throws Exception {
    startServer();
  }

}
