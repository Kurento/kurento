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

package org.kurento.repository.internal;

import javax.servlet.MultipartConfigElement;

import org.kurento.commons.exception.KurentoException;
import org.kurento.repository.Repository;
import org.kurento.repository.RepositoryApiConfiguration;
import org.kurento.repository.internal.repoimpl.filesystem.FileSystemRepository;
import org.kurento.repository.internal.repoimpl.mongo.MongoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class RepositoryApplicationContextConfiguration {

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		return new MultipartConfigElement("");
	}

	@Bean
	public Repository repository() {
		if (repositoryApiConfiguration().getRepositoryType().isFilesystem()) {
			return new FileSystemRepository();
		} else if (repositoryApiConfiguration().getRepositoryType().isMongoDB()) {
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
	public RepositoryApiConfiguration repositoryApiConfiguration() {
		return new RepositoryApiConfiguration();
	}

}