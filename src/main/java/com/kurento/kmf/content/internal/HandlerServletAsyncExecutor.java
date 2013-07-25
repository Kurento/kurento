package com.kurento.kmf.content.internal;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.kurento.kmf.content.ContentApiConfiguration;

public class HandlerServletAsyncExecutor {

	private ThreadPoolExecutor executor;

	@Autowired
	@Qualifier("contentApiConfiguration")
	private ContentApiConfiguration config;

	public HandlerServletAsyncExecutor() {
	}

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
}
