package com.kurento.kmf.content.internal;

import java.lang.annotation.Annotation;
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
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.RtpMediaService;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaService;
import com.kurento.kmf.content.internal.player.PlayerHandlerServlet;
import com.kurento.kmf.content.internal.recorder.RecorderHandlerServlet;
import com.kurento.kmf.content.internal.rtp.RtpMediaHandlerServlet;
import com.kurento.kmf.content.internal.webrtc.WebRtcMediaHandlerServlet;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * This class performs the initialization of the implemented web applications,
 * searching the declared handlers with corresponding annotations (
 * {@link PlayerService}, {@link RecorderService}, {@link RtpMediaService},
 * {@link RtpMediaService}).
 * 
 * @see PlayerService
 * @see RecorderService
 * @see RtpMediaService
 * @see WebRtcMediaService
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
	 * Reflections scans the classpath, indexes the metadata, allows to query it
	 * on runtime and may save and collect that information.
	 */
	private Reflections reflections;

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

		// Initialize ContentApi locating handlers and creating their associated
		// servlets
		reflections = new Reflections("", new TypeAnnotationsScanner());

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
	 * the annotation {@link PlayerService}, and it register a servlet for each
	 * handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which register servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializePlayers(ServletContext sc) throws ServletException {
		for (String ph : findServices(PlayerHandler.class, PlayerService.class)) {
			try {
				PlayerService playerService = Class.forName(ph).getAnnotation(
						PlayerService.class);
				if (playerService != null) {
					String name = playerService.name();
					String path = playerService.path();

					ServletRegistration.Dynamic sr = sc.addServlet(name,
							PlayerHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, ph);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find player class in classpath", e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * Recorder initializator: this method search classes in the classpath using
	 * the annotation {@link RecorderService}, and it register a servlet for
	 * each handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which registering servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializeRecorders(ServletContext sc) throws ServletException {
		for (String rh : findServices(RecorderHandler.class,
				RecorderService.class)) {
			try {
				RecorderService recorderService = Class.forName(rh)
						.getAnnotation(RecorderService.class);
				if (recorderService != null) {
					String name = recorderService.name();
					String path = recorderService.path();
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							RecorderHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, rh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find recorder class in classpath",
						e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * WebRtc initializator: this method search classes in the classpath using
	 * the annotation {@link WebRtcMediaService}, and it register a servlet for
	 * each handler found.
	 * 
	 * @param sc
	 *            Servlet Context in which register servlets for each handler
	 * @throws ServletException
	 *             Exception raised when a reflection problem occurs, typically
	 *             when a class has not been found in the classpath
	 */
	private void initializeWebRtcMediaServices(ServletContext sc)
			throws ServletException {
		for (String wh : findServices(WebRtcMediaHandler.class,
				WebRtcMediaService.class)) {
			try {
				WebRtcMediaService mediaService = Class.forName(wh)
						.getAnnotation(WebRtcMediaService.class);
				if (mediaService != null) {
					String name = mediaService.name();
					String path = mediaService.path();
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							WebRtcMediaHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, wh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find WebRTC class in classpath", e);
				throw new ServletException(e);
			}
		}
	}

	/**
	 * RtpMedia initializator: this method search classes in the classpath using
	 * the annotation {@link RtpMediaService}, and it register a servlet for
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
		for (String wh : findServices(RtpMediaHandler.class,
				RtpMediaService.class)) {
			try {
				RtpMediaService mediaService = Class.forName(wh).getAnnotation(
						RtpMediaService.class);
				if (mediaService != null) {
					String name = mediaService.name();
					String path = mediaService.path();
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							RtpMediaHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(HANDLER_CLASS_PARAM_NAME, wh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find RTP class in classpath", e);
				throw new ServletException(e);
			}
		}

	}

	/**
	 * It seeks declared handlers in the classpath by using reflections.
	 * 
	 * @param handlerClass
	 *            Handler class ({@link PlayerHandler}, {@link RecorderHandler},
	 *            {@link WebRtcMediaHandler}, {@link RtpMediaHandler})
	 * @param serviceAnnotation
	 *            Servide annotation ({@link PlayerService},
	 *            {@link RecorderService}, {@link WebRtcMediaService},
	 *            {@link RtpMediaService})
	 * @return List of services
	 */
	private List<String> findServices(Class<?> handlerClass,
			Class<? extends Annotation> serviceAnnotation) {
		Set<Class<?>> annotatedList = reflections
				.getTypesAnnotatedWith(serviceAnnotation);
		List<String> handlerList = new ArrayList<String>();
		for (Class<?> clazz : annotatedList) {
			if (handlerClass.isAssignableFrom(clazz)) {
				handlerList.add(clazz.getCanonicalName());
			} else {
				log.error("Incorrect implementation of handler: class "
						+ clazz.getCanonicalName() + " is annotated with "
						+ serviceAnnotation.getSimpleName() + " (instead, "
						+ clazz.getSimpleName() + " should extend "
						+ handlerClass.getSimpleName()
						+ " or use the correct annotation)");
			}
		}
		return handlerList;
	}
}
