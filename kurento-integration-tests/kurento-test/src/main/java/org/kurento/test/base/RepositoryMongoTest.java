/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.base;

import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.S3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.Before;
import org.kurento.repository.Repository;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.RepositoryApiConfiguration.RepoType;
import org.kurento.repository.RepositoryHttpPlayer;
import org.kurento.repository.RepositoryItem;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.Protocol;
import org.kurento.test.services.WebServerService;
import org.kurento.test.services.WebServerService.WebServer;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Base for repository tests using Mongo.
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public class RepositoryMongoTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  // Repository
  @ComponentScan(basePackageClasses = { org.kurento.repository.RepositoryItem.class })
  public static class RepositoryWebServer extends WebServer {

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

    @Bean
    public RepositoryApiConfiguration repositoryApiConfiguration() throws UnknownHostException {
      log.info("Repository for playing test");
      RepositoryApiConfiguration config = new RepositoryApiConfiguration();

      config.setWebappPublicUrl("http://" + InetAddress.getLocalHost().getHostAddress() + ":"
          + WebServerService.getAppHttpsPort() + "/");
      config.setMongoDatabaseName("testfiles");
      config.setMongoUrlConnection(Protocol.MONGODB + "://" + getTestFilesMongoPath());
      config.setRepositoryType(RepoType.MONGODB);
      return config;
    }
  }

  public Repository repository;

  @Before
  public void setupRepository() {
    webServer.setWebServerClass(RepositoryWebServer.class);
    repository = (Repository) webServer.getContext().getBean("repository");
  }

  public String getMediaUrl(Protocol protocol, String nameMedia) {
    String mediaUrl = "";
    switch (protocol) {
      case HTTP:
        mediaUrl = HTTP + "://" + getTestFilesHttpPath();
        break;
      case FILE:
        mediaUrl = FILE + "://" + getTestFilesDiskPath();
        break;
      case S3:
        mediaUrl = S3 + "://" + getTestFilesS3Path();
        break;
      case MONGODB:
        List<RepositoryItem> repositoryItem =
        repository.findRepositoryItemsByAttRegex("file", nameMedia);
        RepositoryHttpPlayer repositoryPlayer = repositoryItem.get(0).createRepositoryHttpPlayer();
        mediaUrl = repositoryPlayer.getURL();
        return mediaUrl;
      default:
        throw new RuntimeException(protocol + "is not supported in this test.");
    }
    return mediaUrl + nameMedia;
  }
}
