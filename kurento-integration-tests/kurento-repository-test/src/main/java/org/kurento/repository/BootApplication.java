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

import org.kurento.commons.Address;
import org.kurento.commons.PropertiesManager;
import org.kurento.repository.RepositoryApiConfiguration.RepoType;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.google.common.io.Files;

@ComponentScan
@EnableAutoConfiguration
public class BootApplication {

	private static final String TEST_MONGO_URL_DEFAULT = "mongodb://localhost";
	private static final String TEST_MONGO_URL = "test.mongodb.url";

	public static void main(String[] args) {
		start();
	}

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

		String port = getPort();

		RepositoryApiConfiguration config = new RepositoryApiConfiguration();

		// TODO obtain values from configuration
		RepoType type = RepoType.MONGODB;
		config.setRepositoryType(type);
		if (type.isFilesystem()) {
			config.setFileSystemFolder(Files.createTempDir().getAbsolutePath());
		} else if (type.isMongoDB()) {
			config.setMongoDatabaseName("kurento");
			config.setMongoGridFSCollectionName("fs");
			
			String mongoUrlConnection = PropertiesManager.
					getProperty(TEST_MONGO_URL, TEST_MONGO_URL_DEFAULT);
			
			config.setMongoURLConnection(mongoUrlConnection);
		}
		config.setWebappPublicURL("http://localhost:" + port + "/");

		return config;
	}

	private static String getPort() {
		String port = System.getProperty("repository.port");
		if (port == null) {
			port = "7676";
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
