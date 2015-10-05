/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.repository;

import java.util.Properties;

import org.kurento.repository.internal.RepositoryApplicationContextConfiguration;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan
@EnableAutoConfiguration
@Import(RepositoryApplicationContextConfiguration.class)
public class KurentoRepositoryServerApp {

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

	public static ConfigurableApplicationContext start() {

		Properties properties = new Properties();
		properties.put("server.port",
				RepositoryApplicationContextConfiguration.SERVER_PORT);

		SpringApplication application = new SpringApplication(
				KurentoRepositoryServerApp.class);

		application.setDefaultProperties(properties);

		return application.run();
	}

	public static void main(String[] args) {
		start();
	}
}
