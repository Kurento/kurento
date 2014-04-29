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
package com.kurento.kmf.content.internal;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.kurento.kmf.content.ContentApiConfiguration;

/**
 * Thread pool within Content Management.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class ContentApiExecutorService {

	/**
	 * Thread pool implementation.
	 */
	private ThreadPoolExecutor executor;

	/**
	 * Autowired configuration.
	 */
	@Autowired
	@Qualifier("contentApiConfiguration")
	private ContentApiConfiguration config;

	/**
	 * Default constructor.
	 */
	public ContentApiExecutorService() {
	}

	/**
	 * Post constructor method; instantiate thread pool.
	 * 
	 * @throws Exception
	 *             Error in the creation of the thread pool
	 */
	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		executor = new ThreadPoolExecutor(config.getPoolCoreSize(),
				config.getPoolMaxSize(), config.getPoolExecutionTimeout(),
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(
						config.getPoolMaxQueueSize()),
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
						((RejectableRunnable) r).onExecutionRejected();
					}
				});
	}

	/**
	 * Pre destroy method; shutdown the thread pool.
	 * 
	 * @throws Exception
	 *             Problem while shutting down thread
	 */
	@PreDestroy
	public void destroy() throws Exception {
		executor.shutdown();
	}

	/**
	 * Getter (accessor) of the thread pool (executor).
	 * 
	 * @return Thread pool (executor)
	 */
	public ThreadPoolExecutor getExecutor() {
		return executor;
	}
}
