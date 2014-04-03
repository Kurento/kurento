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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;

import com.kurento.kmf.common.exception.internal.ReflectionUtils;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.HttpRecorderService;
import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.RtpContentService;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.WebRtcContentService;
import com.kurento.kmf.content.internal.player.PlayerHandlerServlet;
import com.kurento.kmf.content.internal.recorder.RecorderHandlerServlet;
import com.kurento.kmf.content.internal.rtp.RtpMediaHandlerServlet;
import com.kurento.kmf.content.internal.webrtc.WebRtcMediaHandlerServlet;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * TODO: review & improve javadoc
 * 
 * This class performs the initialization of the implemented web applications,
 * searching the declared handlers with corresponding annotations (
 * {@link HttpPlayerService}, {@link HttpRecorderService},
 * {@link RtpContentService}, {@link RtpContentService}).
 * 
 * @see HttpPlayerService
 * @see HttpRecorderService
 * @see RtpContentService
 * @see WebRtcContentService
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public class ContentApiWebApplicationInitializer implements
		WebApplicationInitializer {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(ContentApiWebApplicationInitializer.class);

	/**
	 * Identifier for the declared handlers.
	 */
	public static final String HANDLER_CLASS_PARAM_NAME = ContentApiWebApplicationInitializer.class
			.getName() + "HandlerClassParamName";

	/**
	 * Web initialization is performed in this method, calling every handler
	 * initilizator (player, recorder, webrtc, rtp).
	 */
	@Override
	public void onStartup(ServletContext sc) throws ServletException {

		// At this stage we cannot create KurentoApplicationContext given that
		// we don't know y App developer wants to instantiate a Spring root
		// WebApplicationContext
		// ... so we need to live without Spring

		initializeRecorders(sc);
		initializePlayers(sc);
		initializeWebRtcMediaServices(sc);
		initializeRtpMediaServices(sc);

		// Register Kurento ServletContextListener
		KurentoApplicationContextUtils
				.registerKurentoServletContextListener(sc);
	}

	/**
	 * Player initializator: this method search classes in the classpath using
	 * the annotation {@link HttpPlayerService}, and it register a servlet for
	 * each handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which register servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializePlayers(ServletContext sc) throws ServletException {
		for (String ph : findServices(HttpPlayerHandler.class,
				HttpPlayerService.class)) {
			try {
				HttpPlayerService playerService = Class.forName(ph)
						.getAnnotation(HttpPlayerService.class);
				if (playerService != null) {
					String name = playerService.name().isEmpty() ? ph
							: playerService.name();
					String path = playerService.path();
					log.info("Registering HttpPlayerHandler with name " + name
							+ " at path " + path);
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							PlayerHandlerServlet.class);
					if (sr == null) {
						throw new ServletException(
								"Duplicated handler named "
										+ name
										+ " found. You must check your handlers' annotations to assert that no name duplications are declared.");
					}
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, ph);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error(
						"Error: could not find class " + ph + " in classpath",
						e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * Recorder initializator: this method search classes in the classpath using
	 * the annotation {@link HttpRecorderService}, and it register a servlet for
	 * each handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which registering servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializeRecorders(ServletContext sc) throws ServletException {
		for (String rh : findServices(HttpRecorderHandler.class,
				HttpRecorderService.class)) {
			try {
				HttpRecorderService recorderService = Class.forName(rh)
						.getAnnotation(HttpRecorderService.class);
				if (recorderService != null) {
					String name = recorderService.name().isEmpty() ? rh
							: recorderService.name();
					String path = recorderService.path();
					log.debug("Registering HttpRecorderHandler with name "
							+ name + " at path " + path);
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							RecorderHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, rh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error(
						"Error: could not find class " + rh + " in classpath",
						e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * WebRtc initializator: this method search classes in the classpath using
	 * the annotation {@link WebRtcContentService}, and it register a servlet
	 * for each handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which register servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializeWebRtcMediaServices(ServletContext sc)
			throws ServletException {
		for (String wh : findServices(WebRtcContentHandler.class,
				WebRtcContentService.class)) {
			try {
				WebRtcContentService mediaService = Class.forName(wh)
						.getAnnotation(WebRtcContentService.class);
				if (mediaService != null) {
					String name = mediaService.name().isEmpty() ? wh
							: mediaService.name();
					String path = mediaService.path();
					log.debug("Registering WebRtcContentHandler with name "
							+ name + " at path " + path);
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							WebRtcMediaHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, wh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error(
						"Error: could not find class " + wh + " in classpath",
						e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * RtpMedia initializator: this method search classes in the classpath using
	 * the annotation {@link RtpContentService}, and it register a servlet for
	 * each handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which register servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializeRtpMediaServices(ServletContext sc)
			throws ServletException {
		for (String rh : findServices(RtpContentHandler.class,
				RtpContentService.class)) {
			try {
				RtpContentService mediaService = Class.forName(rh)
						.getAnnotation(RtpContentService.class);
				if (mediaService != null) {
					String name = mediaService.name().isEmpty() ? rh
							: mediaService.name();
					String path = mediaService.path();
					log.debug("Registering RtpContentHandler with name " + name
							+ " at path " + path);
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							RtpMediaHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, rh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error(
						"Error: could not find class " + rh + " in classpath",
						e);
				throw new ServletException(e);
			}
		}

	}

	/**
	 * It seeks declared handlers in the classpath by using reflections.
	 * 
	 * @param handlerClass
	 *            Handler class ({@link HttpPlayerHandler},
	 *            {@link HttpRecorderHandler}, {@link WebRtcContentHandler},
	 *            {@link RtpContentHandler})
	 * @param serviceAnnotation
	 *            Service annotation ({@link HttpPlayerService},
	 *            {@link HttpRecorderService}, {@link WebRtcContentService},
	 *            {@link RtpContentService})
	 * @return List of services
	 * @throws ServletException
	 *             Exception raised when an incorrect implementation of handler
	 *             is detected (mismatching between annotation and inheritance
	 *             in handler)
	 */
	private List<String> findServices(Class<?> handlerClass,
			Class<? extends Annotation> serviceAnnotation)
			throws ServletException {
		Set<Class<?>> annotatedList = ReflectionUtils
				.getTypesAnnotatedWith(serviceAnnotation);
		List<String> handlerList = new ArrayList<String>();
		for (Class<?> clazz : annotatedList) {
			if (handlerClass.isAssignableFrom(clazz)) {
				handlerList.add(clazz.getName());
			} else {
				String error = "Incorrect implementation of handler: class "
						+ clazz.getCanonicalName() + " is annotated with "
						+ serviceAnnotation.getSimpleName() + " (instead, "
						+ clazz.getSimpleName() + " should extend "
						+ handlerClass.getSimpleName()
						+ " or use the correct annotation)";
				log.error(error);
				throw new ServletException(error);
			}
		}
		return handlerList;
	}
}
