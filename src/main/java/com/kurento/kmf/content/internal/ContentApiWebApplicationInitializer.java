package com.kurento.kmf.content.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;

import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class ContentApiWebApplicationInitializer implements
		WebApplicationInitializer {

	public static final String PLAYER_HANDLER_CLASS_PARAM_NAME = ContentApiWebApplicationInitializer.class
			.getName() + "playerHandlerClassParamName";

	public static final String RECORDER_HANDLER_CLASS_PARAM_NAME = ContentApiWebApplicationInitializer.class
			.getName() + "recorderHandlerClassParamName";

	private static final Logger log = LoggerFactory
			.getLogger(ContentApiWebApplicationInitializer.class);

	@Override
	public void onStartup(ServletContext sc) throws ServletException {

		// At this stage we cannot create KurentoApplicationContext given that
		// we don't know y App developer wants to instantiate a Spring root
		// WebApplicationContext
		// ... so we need to live without Spring

		// Initialize ContentApi locating handlers and creating their associated
		// servlets
		initializeRecorders(sc);
		initializePlayers(sc);

		// Register Kurento ServletContextListener
		KurentoApplicationContextUtils
				.registerKurentoServletContextListener(sc);
	}

	private void initializePlayers(ServletContext sc) throws ServletException {
		for (String ph : findPlayMappgins()) {
			try {
				PlayerService playerService = Class.forName(ph).getAnnotation(
						PlayerService.class);
				if (playerService != null) {
					String name = playerService.name();
					String path = playerService.path();

					ServletRegistration.Dynamic sr = sc.addServlet(name,
							PlayerHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(PLAYER_HANDLER_CLASS_PARAM_NAME, ph);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find player class in classpath", e);
				throw new ServletException(e);
			}
		}
	}

	private void initializeRecorders(ServletContext sc) throws ServletException {
		for (String ph : findRecordMappgins()) {
			try {
				RecorderService recorderService = Class.forName(ph)
						.getAnnotation(RecorderService.class);
				if (recorderService != null) {
					String name = recorderService.name();
					String path = recorderService.path();
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							RecorderHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(RECORDER_HANDLER_CLASS_PARAM_NAME, ph);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find recorder class in classpath",
						e);
				throw new ServletException(e);
			}
		}
	}

	private List<String> findPlayMappgins() {
		// TODO: perhaps exclude packages? -- add third parameter new
		// FilterBuilder().execute("org.jboss")
		Reflections reflections = new Reflections("",
				new TypeAnnotationsScanner());
		Set<Class<?>> playerList = reflections
				.getTypesAnnotatedWith(PlayerService.class);
		List<String> playerHandlers = new ArrayList<String>();
		for (Class<?> clazz : playerList) {
			if (PlayerHandler.class.isAssignableFrom(clazz)) {
				playerHandlers.add(clazz.getCanonicalName());
			}
		}
		return playerHandlers;
	}

	private List<String> findRecordMappgins() {
		// TODO: perhaps exclude packages? -- add third parameter new
		// FilterBuilder().execute("org.jboss")
		Reflections reflections = new Reflections("",
				new TypeAnnotationsScanner());
		Set<Class<?>> recorderList = reflections
				.getTypesAnnotatedWith(RecorderService.class);
		List<String> recorderHandlers = new ArrayList<String>();
		for (Class<?> clazz : recorderList) {
			if (RecorderHandler.class.isAssignableFrom(clazz)) {
				recorderHandlers.add(clazz.getCanonicalName());
			}
		}
		return recorderHandlers;
	}
}
