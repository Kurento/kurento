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

package org.kurento.client.internal.test;

import java.util.Properties;

import org.kurento.client.internal.transport.jsonrpc.RomServerJsonRpcHandler;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientNettyWebSocket;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

public class WebSocketRomTest extends AbstractRomTest {

  private static Logger log = LoggerFactory.getLogger(WebSocketRomTest.class);

  private static RomServerJsonRpcHandler handler;

  @Configuration
  @ComponentScan(basePackageClasses = JsonRpcConfiguration.class)
  @EnableAutoConfiguration
  public static class BootTestApplication implements JsonRpcConfigurer {

    @Override
    public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
      registry.addHandler(handler, "/handler");
    }
  }

  protected ConfigurableApplicationContext context;

  @Override
  protected JsonRpcClient createJsonRpcClient() {

    String uri = "ws://localhost:" + getPort() + "/handler";
    log.debug("Creating client in URI: " + uri);
    return new JsonRpcClientNettyWebSocket(uri);
  }

  @Override
  protected void startJsonRpcServer(RomServerJsonRpcHandler jsonRpcHandler) {
    handler = jsonRpcHandler;

    Properties properties = new Properties();
    String port = getPort();
    properties.put("server.port", port);

    SpringApplication application = new SpringApplication(BootTestApplication.class);
    application.setDefaultProperties(properties);

    log.debug("Creating server in port: " + port);

    context = application.run();
  }

  protected static String getPort() {
    String port = System.getProperty("http.port");
    if (port == null) {
      port = "7788";
    }
    return port;
  }

  @Override
  protected void destroyJsonRpcServer() {
    context.close();
  }
}
