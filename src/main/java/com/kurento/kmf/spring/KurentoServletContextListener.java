package com.kurento.kmf.spring;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class KurentoServletContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// Nothing to do here given that we don't know if a root
		// WebApplicationContext has been created by application developer
		// because order of execution of listeners cannot be established a
		// priori. Hence, we cannot create the KurentoApplicationContext
		// Note that if a root WebApplicationContext exists it MUST be made the
		// parent of KurnentoApplicationContext
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		KurentoApplicationContextUtils.closeAllKurentoApplicationContexts(arg0
				.getServletContext());
	}
}
