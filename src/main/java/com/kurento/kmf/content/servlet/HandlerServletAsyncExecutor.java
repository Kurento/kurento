package com.kurento.kmf.content.servlet;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

@Component("handlerServletAsyncExecutor")
public class HandlerServletAsyncExecutor {

	private ThreadPoolExecutor executor;
	private int corePoolSize = 10;
	private int maxPoolSize = 200;
	private long executionTimeout = 50000L;
	private int maxQueueSize = 200;

	public HandlerServletAsyncExecutor() {
	}

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		// TODO: use external configuration values
		executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
				executionTimeout, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(maxQueueSize),
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
						((RejectableRunnable) r)
								.reject(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
										"Servler overloaded. Try again in a few minutes");
					}
				});
	}

	@PreDestroy
	public void destroy() throws Exception {
		executor.shutdown();
	}

	public ThreadPoolExecutor getExecutor() {
		return executor;
	}

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public long getExecutionTimeout() {
		return executionTimeout;
	}

	public void setExecutionTimeout(long executionTimeout) {
		this.executionTimeout = executionTimeout;
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}
}
