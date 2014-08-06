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

package org.kurento.kmf.repository.main;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import org.kurento.kmf.repository.RepositoryApiConfiguration;
import org.kurento.kmf.repository.internal.http.RepositoryHttpServlet;

@ComponentScan
@EnableAutoConfiguration
public class BootApplication {

	public static void main(String[] args) {
		start();
	}

	@Bean
	public ServletRegistrationBean repositoryServletRegistrationBean() {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new RepositoryHttpServlet(),
				"/repository_servlet/*");
		servletRegistrationBean.setLoadOnStartup(1);
		return servletRegistrationBean;
	}

	@Bean
	public RepositoryApiConfiguration repositoryApiConfiguration() {

		String port = getPort();

		RepositoryApiConfiguration config = new RepositoryApiConfiguration();
		config.setWebappPublicURL("http://localhost:" + port + "/");
		config.setFileSystemFolder("test-files/repository");
		config.setRepositoryType("filesystem");
		return config;
	}

	private static String getPort() {
		String port = System.getProperty("repository.port");
		if (port == null) {
			port = "7779";
		}
		return port;
	}

	public static ConfigurableApplicationContext start() {

		Properties properties = new Properties();
		properties.put("server.port", getPort());

		SpringApplication application = new SpringApplication(
				BootApplication.class);

		application.setDefaultProperties(properties);

		return application.run();
	}
}
