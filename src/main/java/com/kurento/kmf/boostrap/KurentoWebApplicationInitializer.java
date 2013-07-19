package com.kurento.kmf.boostrap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

import com.kurento.kmf.content.servlet.ContentApiInitializer;
import com.kurento.kmf.spring.KurentoServletContextListener;

public class KurentoWebApplicationInitializer implements
		WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext sc) throws ServletException {

		// At this stage we cannot create KurentoApplicationContext given that
		// we don't know y App developer wants to instantiate a Spring root
		// WebApplicationContext
		// ... so we need to live without Spring

		// Add listener, which will close all Kurento ApplicationContexts when
		// appropriate
		sc.addListener(KurentoServletContextListener.class);

		// Initialize ContentApi locating handlers and creating their associated
		// servlets
		ContentApiInitializer contentApiInitializer = new ContentApiInitializer();
		contentApiInitializer.initializePlayers(sc);
		contentApiInitializer.initializeRecorders(sc);

		// TODO initialize rest of managers
	}
}
