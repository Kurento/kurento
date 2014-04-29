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
import org.springframework.context.annotation.ComponentScan;

import com.kurento.kmf.content.internal.ContentApiWebApplicationInitializer;
import com.kurento.kmf.spring.KurentoServletContextListener;

/**
 * Initializer class to allow execute tests with Spring Boot.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 * @see <a href="http://projects.spring.io/spring-boot/">Spring Boot</a>
 */
@ComponentScan
@EnableAutoConfiguration
public class BootApplication {

	@Bean
	public ServletContextInitializer webApplicationInitializer() {
		return new ServletContextInitializer() {

			@Override
			public void onStartup(ServletContext servletContext)
					throws ServletException {

				ContentApiWebApplicationInitializer init = new ContentApiWebApplicationInitializer();
				init.onStartup(servletContext);

			}
		};
	}

	@Bean
	public ServletListenerRegistrationBean<KurentoServletContextListener> listener() {
		return new ServletListenerRegistrationBean<>(
				new KurentoServletContextListener());
	}

	public static ConfigurableApplicationContext start() {
		Properties properties = new Properties();
		properties.put("server.port", PortManager.getPort());

		SpringApplication application = new SpringApplication(
				BootApplication.class);

		application.setDefaultProperties(properties);

		return application.run();
	}
}
