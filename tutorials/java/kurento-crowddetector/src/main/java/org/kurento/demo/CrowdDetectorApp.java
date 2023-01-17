/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.demo;

import java.io.IOException;

import org.kurento.client.KurentoClient;
import org.kurento.orion.OrionConnector;
import org.kurento.orion.OrionConnectorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * CrowdDetector with RTSP media source (application and media logic).
 *
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 5.0.4
 */
@SpringBootApplication
@EnableWebSocket
public class CrowdDetectorApp implements WebSocketConfigurer {

  static final String DEFAULT_CONFIG_FILE_PATH = "/config/configuration.conf.json";

  @Bean
  public ConfigurationReader init() throws IOException {
    return new ConfigurationReader(System.getProperty("app.configFile", DEFAULT_CONFIG_FILE_PATH));
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  @Bean
  public OrionConnector orionConnector() {
    return new OrionConnector();
  }

  @Bean
  public OrionConnectorConfiguration orionConnectorConfiguration() {
    return new OrionConnectorConfiguration();
  }

  @Bean
  public CrowdDetectorOrionPublisher crowdDetectorOrionPublisher() {
    return new CrowdDetectorOrionPublisher();
  }

  @Bean
  public Pipeline pipeline() {
    return new Pipeline();
  }

  @Bean
  public CrowdDetectorHandler handler() {
    return new CrowdDetectorHandler();
  }

  @Bean
  public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(32768);
    return container;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler(), "/crowddetector");
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(CrowdDetectorApp.class, args);
  }
}
