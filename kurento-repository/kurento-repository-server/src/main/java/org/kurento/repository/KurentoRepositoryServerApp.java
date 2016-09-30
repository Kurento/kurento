/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.repository;

import java.util.Properties;

import org.kurento.repository.internal.RepositoryApplicationContextConfiguration;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RepositoryApplicationContextConfiguration.class)
public class KurentoRepositoryServerApp {

  @Bean
  public RepositoryHttpServlet repositoryHttpServlet() {
    return new RepositoryHttpServlet();
  }

  @Bean
  public ServletRegistrationBean repositoryServletRegistrationBean(
      RepositoryHttpServlet repositoryHttpServlet) {

    ServletRegistrationBean servletRegistrationBean =
        new ServletRegistrationBean(repositoryHttpServlet, "/repository_servlet/*");
    servletRegistrationBean.setLoadOnStartup(1);

    return servletRegistrationBean;
  }

  public static ConfigurableApplicationContext start() {

    Properties properties = new Properties();
    properties.put("server.port", RepositoryApplicationContextConfiguration.SERVER_PORT);

    SpringApplication application = new SpringApplication(KurentoRepositoryServerApp.class);

    application.setDefaultProperties(properties);

    return application.run();
  }

  public static void main(String[] args) {
    start();
  }
}
