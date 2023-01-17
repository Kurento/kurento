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

package org.kurento.test.services;

import static org.kurento.test.config.TestConfiguration.APP_HTTPS_PORT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.APP_HTTPS_PORT_PROP;
import static org.kurento.test.config.TestConfiguration.APP_HTTP_PORT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.APP_HTTP_PORT_PROP;
import static org.kurento.test.services.TestService.TestServiceScope.TEST;

import org.apache.catalina.connector.Connector;
import org.kurento.commons.PropertiesManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Web server service.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class WebServerService extends TestService {

  @EnableAutoConfiguration
  public static class WebServer {

    @Bean
    @ConditionalOnMissingBean
    public ServletWebServerFactory servletContainer() {
    	TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
      Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
      connector.setScheme("http");
      connector.setPort(getAppHttpPort());

      tomcat.addAdditionalTomcatConnectors(connector);
      return tomcat;
    }
  }

  private Class<?> webServerClass;

  protected ConfigurableApplicationContext context;

  public WebServerService() {
    this.webServerClass = WebServer.class;
  }

  public WebServerService(Class<?> webServerClass) {
    this.webServerClass = webServerClass;
  }

  @Override
  public void start() {
    super.start();

    System.setProperty("java.security.egd", "file:/dev/./urandom");
    startContext();
  }

  private void startContext() {
    context = new SpringApplication(webServerClass).run("--server.port=" + getAppHttpsPort());
    context.registerShutdownHook();
  }

  @Override
  public void stop() {
    super.stop();

    stopContext();
  }

  private void stopContext() {
    if (context != null && context.isRunning()) {
      context.stop();
      context.close();
    }
  }

  @Override
  public TestServiceScope getScope() {
    return TEST;
  }

  public ConfigurableApplicationContext getContext() {
    return context;
  }

  public static int getAppHttpsPort() {
    return PropertiesManager.getProperty(APP_HTTPS_PORT_PROP, APP_HTTPS_PORT_DEFAULT);
  }

  public static int getAppHttpPort() {
    return PropertiesManager.getProperty(APP_HTTP_PORT_PROP, APP_HTTP_PORT_DEFAULT);
  }

  public void setWebServerClass(Class<?> webServerClass) {
    this.webServerClass = webServerClass;

    stopContext();
    startContext();
  }

}
