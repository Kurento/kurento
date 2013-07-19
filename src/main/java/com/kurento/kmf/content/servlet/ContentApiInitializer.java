package com.kurento.kmf.content.servlet;

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

import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerMapping;

public class ContentApiInitializer {

	public static final String PLAYER_HANDLER_CLASS_PARAM_NAME = ContentApiInitializer.class
			.getName() + "playerHandlerClassParamName";

	private static final Logger log = LoggerFactory
			.getLogger(ContentApiInitializer.class);

	public void initializePlayers(ServletContext sc) throws ServletException {
		for (String ph : findPlayMappgins()) {
			try {
				PlayerMapping playerMapping = Class.forName(ph).getAnnotation(
						PlayerMapping.class);
				if (playerMapping != null) {
					String name = playerMapping.name();
					String path = playerMapping.path();

					ServletRegistration.Dynamic sr = sc.addServlet(name,
							PlayerHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(PLAYER_HANDLER_CLASS_PARAM_NAME, ph);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error(
						"Error finding annotations in ContentServletContextListener",
						e);
				throw new ServletException(e);
			}
		}
	}

	public void initializeRecorders(ServletContext sc) throws ServletException {
		// TODO to implement
	}

	private List<String> findPlayMappgins() {
		Reflections reflections = new Reflections("",
				new TypeAnnotationsScanner()); // TODO perhaps exclude packages?
												// -- add third parameter new
												// FilterBuilder().execute("org.jboss")
		Set<Class<?>> playerList = reflections
				.getTypesAnnotatedWith(PlayerMapping.class);
		List<String> playerHandlers = new ArrayList<String>();
		for (Class<?> clazz : playerList) {
			if (PlayerHandler.class.isAssignableFrom(clazz)) {
				playerHandlers.add(clazz.getCanonicalName());
			}
		}
		return playerHandlers;
	}
}
