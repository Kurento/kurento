/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
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
 */
package org.kurento.tutorial.helloworld;

import org.kurento.client.KurentoClient;
import org.kurento.repository.RepositoryClient;
import org.kurento.repository.RepositoryClientProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebRTC in loopback with recording in repository capabilities main class.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.1.1
 */
@SpringBootApplication
@EnableWebSocket
public class HelloWorldRecApp implements WebSocketConfigurer {

  protected static final String DEFAULT_REPOSITORY_SERVER_URI = "http://localhost:7676";

  protected static final String REPOSITORY_SERVER_URI = System.getProperty("repository.uri",
      DEFAULT_REPOSITORY_SERVER_URI);

  @Bean
  public HelloWorldRecHandler handler() {
    return new HelloWorldRecHandler();
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  @Bean
  public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(32768);
    return container;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler(), "/repository");
  }

  @Bean
  public RepositoryClient repositoryServiceProvider() {
    return REPOSITORY_SERVER_URI.startsWith("file://") ? null
        : RepositoryClientProvider.create(REPOSITORY_SERVER_URI);
  }

  @Bean
  public UserRegistry registry() {
    return new UserRegistry();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HelloWorldRecApp.class, args);
  }
}
