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
package com.kurento.kmf.thrift.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;

/**
 * Thread pool within Media-API.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 */
@Component
public class ThriftInterfaceExecutorService {

	private static final Logger log = LoggerFactory
			.getLogger(ThriftInterfaceExecutorService.class);

	/**
	 * Thread pool implementation.
	 */
	private ThreadPoolExecutor executor;

	/**
	 * Autowired configuration.
	 */
	@Autowired
	private ThriftInterfaceConfiguration config;

	/**
	 * Default constructor, to be used in spring environments
	 */
	public ThriftInterfaceExecutorService() {
	}

	/**
	 * Constructor for non-spring environments.
	 * 
	 * @param config
	 *            configuration object
	 */
	public ThriftInterfaceExecutorService(ThriftInterfaceConfiguration config) {
		this.config = config;
		afterPropertiesSet();
	}

	/**
	 * Post constructor method; instantiate thread pool.
	 */
	@PostConstruct
	public void afterPropertiesSet() {
		executor = new ThreadPoolExecutor(config.getPoolCoreSize(),
				config.getPoolMaxSize(), config.getPoolExecutionTimeout(),
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(
						config.getPoolMaxQueueSize()),
				new RejectedExecutionHandler() {

					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor exec) {
						log.warn("Execution is blocked because the thread bounds and queue capacities are reached");
					}
				});
	}

	/**
	 * Pre-destroy method; shutdown the thread pool.
	 * 
	 * @throws SecurityException
	 *             if a security manager exists and shutting down this
	 *             ExecutorService may manipulate threads that the caller is not
	 *             permitted to modify because it does not hold
	 *             {@link java.lang.RuntimePermission}{@code ("modifyThread")},
	 *             or the security manager's {@code checkAccess} method denies
	 *             access.
	 */
	@PreDestroy
	public void destroy() {
		executor.shutdown();
	}

	/**
	 * Getter (accessor) of the thread pool (executor).
	 * 
	 * @return Thread pool (executor)
	 */
	public ExecutorService getExecutor() {
		return executor;
	}
}
