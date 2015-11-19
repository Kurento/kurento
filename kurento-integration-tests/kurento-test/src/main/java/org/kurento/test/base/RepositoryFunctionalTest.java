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

import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.repository.Repository;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.RepositoryApiConfiguration.RepoType;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
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
public class RepositoryFunctionalTest
		extends KurentoClientBrowserTest<WebRtcTestPage> {

	@ComponentScan(basePackageClasses = {
			org.kurento.repository.RepositoryItem.class })
	public static class RepositoryWebServer extends WebServer {

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
			config.setWebappPublicURL("http://localhost:"
					+ KurentoServicesTestHelper.getAppHttpPort() + "/");
			config.setFileSystemFolder(Files.createTempDir().toString());
			config.setRepositoryType(RepoType.FILESYSTEM);
			return config;
		}
	}

	public Repository repository;

	public RepositoryFunctionalTest(TestScenario testScenario) {
		super(testScenario);
		setWebServerClass(RepositoryWebServer.class);
	}

	@Before
	public void setupHttpServer() throws Exception {
		repository = (Repository) context.getBean("repository");
	}
}
