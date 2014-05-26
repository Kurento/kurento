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
package com.kurento.kmf.test;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kurento.kmf.content.internal.ContentApiWebApplicationInitializer;
import com.kurento.kmf.media.factory.KmfMediaApi;
import com.kurento.kmf.media.factory.MediaPipelineFactory;
import com.kurento.kmf.spring.KurentoServletContextListener;
import com.kurento.kmf.test.services.KurentoServicesTestHelper;

/**
 * Initializer class to allow execute tests with Spring Boot.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
@Configuration
@EnableAutoConfiguration
public class ContentApiBootApp {

	@Bean
	public ServletContextInitializer webApplicationInitializer() {
		return new ServletContextInitializer() {
			@Override
			public void onStartup(ServletContext servletContext)
					throws ServletException {

				new ContentApiWebApplicationInitializer()
						.onStartup(servletContext);
			}
		};
	}

	@Bean
	public ServletListenerRegistrationBean<KurentoServletContextListener> listener() {
		return new ServletListenerRegistrationBean<>(
				new KurentoServletContextListener());
	}

	@Bean
	public MediaPipelineFactory mediaPipelineFactory() {
		return KmfMediaApi.createMediaPipelineFactoryFromSystemProps();
	}

	public static ConfigurableApplicationContext start() {

		SpringApplication application = new SpringApplication(
				ContentApiBootApp.class);

		Properties properties = new Properties();
		// TODO: Make this configurable with system properties
		properties.put("server.port",
				KurentoServicesTestHelper.getAppHttpPort());
		application.setDefaultProperties(properties);

		return application.run();
	}
}
