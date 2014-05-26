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
package com.kurento.kmf.spring;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.kurento.kmf.common.exception.KurentoException;

public final class KurentoApplicationContextUtils {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoApplicationContextUtils.class);

	private static final String KURENTO_SERVLET_CONTEXT_LISTENER_ATTRIBUTE_NAME = KurentoApplicationContextUtils.class
			+ "AttributeName";

	// Is there any better mechanisms for enabling direct recovery of beans
	// (e.g. configurations)
	private static AnnotationConfigApplicationContext kurentoApplicationContextInternalReference;
	private static ConcurrentHashMap<String, AnnotationConfigApplicationContext> childContexts;

	private KurentoApplicationContextUtils() {
	}

	/**
	 * This class returns the Spring KurentoApplicationContext, which is the
	 * parent context for all specific Kurento Servlet contexts. In case a
	 * pre-exiting Spring root WebApplicationContext if found, the returned
	 * KurentoApplicationContext will be made child of this root context. When
	 * necessary, this method creates the KurentoApplicationContext, so it
	 * should never return null.
	 *
	 * This method MUST NOT be called in ServletContextListeners, given that at
	 * that stage there might not be information about the presence of a root
	 * Spring root WebApplicationConext.
	 *
	 * @param ctx
	 * @return the context
	 *
	 */
	public static AnnotationConfigApplicationContext createKurentoApplicationContext(
			ServletContext ctx) {
		Assert.notNull(ctx,
				"Cannot recover KurentoApplicationContext from a null ServletContext");
		Assert.isNull(kurentoApplicationContextInternalReference,
				"Pre-existing Kurento ApplicationContext found. Cannot create a new instance.");

		kurentoApplicationContextInternalReference = new AnnotationConfigApplicationContext();

		// We can't scan whole com.kurento.kmf package because there are classes
		// in classpath not designed to work with content-api
		kurentoApplicationContextInternalReference
		.scan("com.kurento.kmf.spring");
		kurentoApplicationContextInternalReference
		.scan("com.kurento.kmf.content");
		kurentoApplicationContextInternalReference
				.scan("com.kurento.kmf.repository");

		// Recover root WebApplicationContext context just in case
		// application developer is using Spring
		WebApplicationContext rootContext = WebApplicationContextUtils
				.getWebApplicationContext(ctx);
		if (rootContext != null) {
			kurentoApplicationContextInternalReference.setParent(rootContext);
		}

		final String jbossServerConfigDir = System
				.getProperty("jboss.server.config.dir");
		final String kurentoPropertiesDir = System
				.getProperty("kurento.properties.dir");
		final String kurentoProperties = "/kurento.properties";

		InputStream inputStream = null;
		try {
			if (jbossServerConfigDir != null
					&& new File(jbossServerConfigDir + kurentoProperties)
							.exists()) {
				// First, look for JVM argument "jboss.server.config.dir"
				log.info("Found custom properties in 'jboss.server.config.dir': "
						+ jbossServerConfigDir);
				inputStream = new FileInputStream(jbossServerConfigDir
						+ kurentoProperties);

			} else if (kurentoPropertiesDir != null
					&& new File(kurentoPropertiesDir + kurentoProperties)
							.exists()) {
				// Second, look for JVM argument "kurento.properties.dir"
				log.info("Found custom properties in 'kurento.properties.dir': "
						+ kurentoPropertiesDir);
				inputStream = new FileInputStream(kurentoPropertiesDir
						+ kurentoProperties);
			} else {
				// Third, look for properties in Servlet Context
				ServletContextResource servletContextResource = new ServletContextResource(
						ctx, "/WEB-INF" + kurentoProperties);
				if (servletContextResource.exists()) {
					log.info("Found custom properties in Servlet Context: /WEB-INF"
							+ kurentoProperties);
					inputStream = servletContextResource.getInputStream();
				}
			}

			if (inputStream != null) {
				Properties properties = new Properties();
				properties.load(inputStream);
				PropertyOverrideConfigurer propertyOverrideConfigurer = new PropertyOverrideConfigurer();
				propertyOverrideConfigurer.setProperties(properties);
				kurentoApplicationContextInternalReference
						.addBeanFactoryPostProcessor(propertyOverrideConfigurer);
				inputStream.close();
			}

		} catch (IOException e) {
			throw new KurentoException("Exception loading custom properties", e);
		}

		kurentoApplicationContextInternalReference.refresh();
		return kurentoApplicationContextInternalReference;
	}

	public static boolean kurentoApplicationContextExists() {
		return kurentoApplicationContextInternalReference != null;
	}

	public static AnnotationConfigApplicationContext getKurentoApplicationContext() {
		return kurentoApplicationContextInternalReference;
	}

	/**
	 * Returns a specific application context associated to a Kurento handler
	 * servlet. This method returns null if the context does not exist.
	 *
	 * @param servletClass
	 * @param servletName
	 * @return the context
	 */
	public static AnnotationConfigApplicationContext getKurentoServletApplicationContext(
			Class<?> servletClass, String servletName) {
		Assert.notNull(servletClass,
				"Cannot recover KurentoServletApplicationContext from a null Servlet class");

		if (childContexts == null) {
			return null;
		}

		AnnotationConfigApplicationContext childAppContext = childContexts
				.get(servletClass.getName() + ":" + servletName);
		if (childAppContext == null) {
			return null;
		}

		return childAppContext;
	}

	public static AnnotationConfigApplicationContext createKurentoHandlerServletApplicationContext(
			Class<?> servletClass, String servletName, ServletContext sc,
			String handlerClassName) {
		Assert.notNull(sc,
				"Cannot create Kurento ServletApplicationContext from null ServletContext");
		Assert.notNull(servletClass,
				"Cannot create KurentoServletApplicationContext from a null Servlet class");
		Assert.notNull(servletClass,
				"Cannot create KurentoServletApplicationContext from a null Hanlder class");

		if (childContexts == null) {
			childContexts = new ConcurrentHashMap<>();
		}

		AnnotationConfigApplicationContext childContext = childContexts
				.get(servletClass.getName() + ":" + servletName);
		Assert.isNull(childContext,
				"Pre-existing context found associated to servlet class "
						+ servletClass.getName() + " and servlet name "
						+ servletName);

		childContext = new AnnotationConfigApplicationContext();
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClassName(handlerClassName);
		childContext.registerBeanDefinition(handlerClassName, beanDefinition);
		if (!kurentoApplicationContextExists()) {
			createKurentoApplicationContext(sc);
		}
		childContext.setParent(getKurentoApplicationContext());
		childContext.refresh();
		childContexts.put(servletClass.getName(), childContext);

		return childContext;
	}

	public static void closeAllKurentoApplicationContexts(ServletContext ctx) {
		Assert.notNull(ctx, "Cannot close contexts from a null ServletContext");

		if (childContexts != null) {
			for (AnnotationConfigApplicationContext childContext : childContexts
					.values()) {
				log.info("Closing Kurento Servlet Application Context "
						+ childContext);
				childContext.close();
			}
		}
		childContexts = null;

		if (kurentoApplicationContextInternalReference != null) {
			log.info("Closing Kurento Application Context "
					+ kurentoApplicationContextInternalReference);
			kurentoApplicationContextInternalReference.close();
		}
		kurentoApplicationContextInternalReference = null;
	}

	public static void processInjectionBasedOnApplicationContext(Object bean,
			AnnotationConfigApplicationContext appContext) {
		Assert.notNull(
				appContext,
				"Cannot process bean injection. Reason the specified ApplicationContext is null");
		Assert.notNull(bean,
				"Cannot process bean injection into null bean reference");
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(appContext.getAutowireCapableBeanFactory());
		bpp.processInjection(bean);
	}

	public static void processInjectionBasedOnKurentoApplicationContext(
			Object bean) {
		Assert.notNull(
				kurentoApplicationContextInternalReference,
				"Cannot process bean injection. Reason Kurento ApplicationContext has not been initialized");
		Assert.notNull(bean,
				"Cannot process bean injection into null bean reference");
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(kurentoApplicationContextInternalReference
				.getAutowireCapableBeanFactory());
		bpp.processInjection(bean);
	}

	public static <T> T getConfiguration(Class<T> configurationClass) {
		Assert.notNull(kurentoApplicationContextInternalReference,
				"Cannot access configuration before creating Kurento Application Context");

		T result = kurentoApplicationContextInternalReference
				.getBean(configurationClass);
		Assert.notNull(result,
				"No configuration has been found associated to type "
						+ configurationClass.getName());

		return result;
	}

	public static Object getBean(String name, Object... args) {
		Assert.notNull(
				kurentoApplicationContextInternalReference,
				"Cannot get bean for the following reason: Kurento ApplicationContext has not been initlized.");
		return kurentoApplicationContextInternalReference.getBean(name, args);
	}

	public static Object getBean(String name) {
		Assert.notNull(
				kurentoApplicationContextInternalReference,
				"Cannot get bean for the following reason: Kurento ApplicationContext has not been initlized.");
		return kurentoApplicationContextInternalReference.getBean(name);

	}

	public static void registerKurentoServletContextListener(ServletContext ctx) {
		// Add listener for closing Kurento ApplicationContexts on container
		// shutdown

		if (ctx.getAttribute(KURENTO_SERVLET_CONTEXT_LISTENER_ATTRIBUTE_NAME) != null) {
			log.info("Kurento ServletContextListener already registered, we don't register it again ...");
			return;
		}
		log.info("Registering Kurento ServletContextListener ...");
		ctx.setAttribute(KURENTO_SERVLET_CONTEXT_LISTENER_ATTRIBUTE_NAME,
				"initialized");

		try {
			ctx.addListener(KurentoServletContextListener.class);
		} catch (NullPointerException e) {
			// TODO: Workaround to make it compatible with/without SpringBoot.
			// This exception is thrown when this class is used with SpringBoot.
			// As workaround, SpringBoot application has to define the following
			// @Bean in a @Configuration class:
			//
			// @Bean
			// public ServletListenerRegistrationBean
			// <KurentoServletContextListener> listener(){
			// return new ServletListenerRegistrationBean<>(
			// new KurentoServletContextListener());
			// }
		}
	}

	public static AnnotationConfigApplicationContext debugOnlyCreateKurentoApplicationContext() {
		Assert.isNull(kurentoApplicationContextInternalReference,
				"Pre-existing Kurento ApplicationContext found. Cannot create a new instance.");

		kurentoApplicationContextInternalReference = new AnnotationConfigApplicationContext();

		// Add or remove packages when required
		kurentoApplicationContextInternalReference.scan("com.kurento.kmf");

		kurentoApplicationContextInternalReference.refresh();
		return kurentoApplicationContextInternalReference;
	}
}
