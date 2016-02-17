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
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
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
    public EmbeddedServletContainerFactory servletContainer() {
      TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
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
