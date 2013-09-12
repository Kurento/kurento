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
