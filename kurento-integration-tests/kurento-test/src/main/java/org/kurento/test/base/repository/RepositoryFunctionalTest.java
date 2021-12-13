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

package org.kurento.test.base.repository;

import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.repository.Repository;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.RepositoryApiConfiguration.RepoType;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.services.WebServerService;
import org.kurento.test.services.WebServerService.WebServer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.google.common.io.Files;

/**
 * Base for repository tests.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.4
 */
@Category(SystemFunctionalTests.class)
public class RepositoryFunctionalTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  @ComponentScan(basePackageClasses = { org.kurento.repository.RepositoryItem.class })
  public static class RepositoryWebServer extends WebServer {

    @Bean
    public RepositoryHttpServlet repositoryHttpServlet() {
      return new RepositoryHttpServlet();
    }

    @Bean
    public ServletRegistrationBean<?> repositoryServletRegistrationBean(
        RepositoryHttpServlet repositoryHttpServlet) {
      ServletRegistrationBean<?> servletRegistrationBean =
          new ServletRegistrationBean<>(repositoryHttpServlet, "/repository_servlet/*");
      servletRegistrationBean.setLoadOnStartup(1);
      return servletRegistrationBean;
    }

    @Bean
    public RepositoryApiConfiguration repositoryApiConfiguration() {
      RepositoryApiConfiguration config = new RepositoryApiConfiguration();
      config.setWebappPublicUrl("http://localhost:" + WebServerService.getAppHttpsPort() + "/");
      config.setFileSystemFolder(Files.createTempDir().toString());
      config.setRepositoryType(RepoType.FILESYSTEM);
      return config;
    }
  }

  public Repository repository;

  @Before
  public void setupRepository() {
    webServer.setWebServerClass(RepositoryWebServer.class);
    repository = (Repository) webServer.getContext().getBean("repository");
  }

}
