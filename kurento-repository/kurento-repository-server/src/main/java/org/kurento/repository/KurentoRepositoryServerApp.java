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

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Properties;

import org.kurento.commons.ConfigFileManager;
import org.kurento.repository.RepositoryApiConfiguration.RepoType;
import org.kurento.repository.internal.http.RepositoryHttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

/**
 * Kurento Repository web application (it's a Spring app).
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
@SpringBootApplication
public class KurentoRepositoryServerApp {

	static {
		ConfigFileManager.loadConfigFile("repository.conf.json");
	}

	// TODO remove if not used
	private static final Logger log = LoggerFactory
			.getLogger(KurentoRepositoryServerApp.class);

	public static int SERVER_PORT = getProperty("repository.port", 7676);
	public static String SERVER_HOSTNAME = getProperty("repository.hostname",
			"localhost");
	public static String REPO_TYPE = getProperty("repository.type",
			RepoType.FILESYSTEM.getTypeValue());

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
	@Lazy
	public RepositoryApiConfiguration repositoryApiConfiguration() {
		RepositoryApiConfiguration config = new RepositoryApiConfiguration();
		config.setWebappPublicURL("http://" + SERVER_HOSTNAME + ":"
				+ SERVER_PORT + "/");
		RepoType type = RepoType.parseType(REPO_TYPE);
		config.setRepositoryType(type);
		if (type.isFilesystem()) {
			String filesFolder = getProperty("repository.filesystem.folder",
					config.getFileSystemFolder());
			config.setFileSystemFolder(filesFolder);
		} else if (type.isMongoDB()) {
			String dbName = getProperty("repository.mongodb.dbName",
					config.getMongoDatabaseName());
			config.setMongoDatabaseName(dbName);
			String grid = getProperty("repository.mongodb.gridName",
					config.getMongoGridFSCollectionName());
			config.setMongoGridFSCollectionName(grid);
			String url = getProperty("repository.mongodb.urlConn",
					config.getMongoURLConnection());
			config.setMongoURLConnection(url);
		}
		return config;
	}

	public static ConfigurableApplicationContext start() {
		SpringApplication application = new SpringApplication(
				KurentoRepositoryServerApp.class);

		Properties properties = new Properties();
		properties.put("server.port", SERVER_PORT);
		application.setDefaultProperties(properties);

		return application.run();
	}
}
