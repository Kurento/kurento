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

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.kurento.commons.testing.SystemKurentoClientTests;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.google.common.io.Files;

/**
 * Base for repository tests.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.4
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { BrowserKurentoClientTest.class,
		RepositoryKurentoClientTest.class })
@WebAppConfiguration
@IntegrationTest("server.port:"
		+ KurentoServicesTestHelper.APP_HTTP_PORT_DEFAULT)
@ComponentScan(basePackages = { "org.kurento.repository" })
@EnableAutoConfiguration
@Category(SystemKurentoClientTests.class)
public class RepositoryKurentoClientTest extends KurentoClientTest {

	@Bean
	public RepositoryHttpServlet repositoryHttpServlet() {
		return new RepositoryHttpServlet();
	}

	@Bean
	public ServletRegistrationBean repositoryServletRegistrationBean(
			RepositoryHttpServlet repositoryHttpServlet) {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(
				repositoryHttpServlet, "/repository_servlet/*");
		servletRegistrationBean.setLoadOnStartup(1);
		return servletRegistrationBean;
	}

	@Bean
	public RepositoryApiConfiguration repositoryApiConfiguration() {
		RepositoryApiConfiguration config = new RepositoryApiConfiguration();
		config.setWebappPublicURL("http://localhost:" + getServerPort() + "/");
		config.setFileSystemFolder(Files.createTempDir().toString());
		config.setRepositoryType("filesystem");
		return config;
	}
}
