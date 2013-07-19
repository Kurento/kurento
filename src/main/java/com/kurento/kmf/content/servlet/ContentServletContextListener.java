package com.kurento.kmf.content.servlet;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletResponse;

@WebListener
public class ContentServletContextListener implements ServletContextListener {

	private ThreadPoolExecutor executor;
	static final String EXECUTOR = "executor";

	// Public constructor is required by servlet spec
	public ContentServletContextListener() {
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// TODO: use external configuration values
		executor = new ThreadPoolExecutor(20, 100, 50000L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(200),
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
						((RejectableRunnable) r)
								.reject(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
										"Servler overloaded. Try again in a few minutes");
					}
				});
		sce.getServletContext().setAttribute(EXECUTOR, executor);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		if (executor != null) {
			executor.shutdown();
		}
	}

}
