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

package com.kurento.kmf.repository.internal;

import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.repository.Repository;
import com.kurento.kmf.repository.RepositoryApiConfiguration;
import com.kurento.kmf.repository.internal.repoimpl.filesystem.FileSystemRepository;
import com.kurento.kmf.repository.internal.repoimpl.mongo.MongoRepository;
import com.kurento.kmf.spring.RootWebApplicationContextParentRecoverer;

@Configuration
public class RepositoryApplicationContextConfiguration {

	private static final Logger log = LoggerFactory
			.getLogger(RepositoryApplicationContextConfiguration.class);

	@Autowired
	private RootWebApplicationContextParentRecoverer parentRecoverer;

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		return new MultipartConfigElement("");
	}

	@Bean
	public Repository repository() {
		if (repositoryApiConfiguration().getRepositoryType().equals(
				"filesystem")) {
			return new FileSystemRepository();
		} else if (repositoryApiConfiguration().getRepositoryType().equals(
				"mongodb")) {
			return new MongoRepository();
		} else {
			throw new KurentoException(
					"Unrecognized repository type. Must be filesystem or mongodb");
		}
	}

	@Bean(destroyMethod = "shutdown")
	public TaskScheduler repositoryTaskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Bean
	@Primary
	public RepositoryApiConfiguration repositoryApiConfiguration() {
		try {
			return parentRecoverer.getParentContext().getBean(
					RepositoryApiConfiguration.class);
		} catch (NullPointerException npe) {
			log.info("Configuring Repository API. Could not find parent context. Switching to default configuration ...");
		} catch (NoSuchBeanDefinitionException t) {
			log.info(
					"Configuring Repository API. Could not find exacly one bean of class {}. Switching to default configuration ...",
					RepositoryApiConfiguration.class.getSimpleName());
		}
		return new RepositoryApiConfiguration();
	}

}