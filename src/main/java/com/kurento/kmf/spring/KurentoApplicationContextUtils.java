package com.kurento.kmf.spring;

import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class KurentoApplicationContextUtils {
	public static final String KURENTO_APPLICATION_CONTEXT_ATT_NAME = KurentoApplicationContextUtils.class
			.getName() + "KurentoApplicationContextAttributeName";
	public static final String KURENTO_SERVLET_APPLICATION_CONTEXTS_ATT_NAME = KurentoApplicationContextUtils.class
			.getName() + "KurentoServletApplicationContextsAttributeName";

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
	 * @return
	 * 
	 */
	public static AnnotationConfigApplicationContext getKurentoApplicationContext(
			ServletContext ctx) {
		Assert.notNull(ctx,
				"Cannot recover KurentoApplicationContext from a null ServletContext");
		AnnotationConfigApplicationContext appContext = (AnnotationConfigApplicationContext) ctx
				.getAttribute(KURENTO_APPLICATION_CONTEXT_ATT_NAME);

		if (appContext == null) {
			// Create application context
			appContext = new AnnotationConfigApplicationContext();
			appContext.scan("com.kurento.kmf.content"); // TODO add other
														// packages

			// Recover root WebApplicationContext context just in case
			// application developer is using Spring
			WebApplicationContext rootContext = WebApplicationContextUtils
					.getWebApplicationContext(ctx);
			if (rootContext != null) {
				appContext.setParent(rootContext);
			}
			appContext.refresh();
			ctx.setAttribute(KURENTO_APPLICATION_CONTEXT_ATT_NAME, appContext);
		}

		return appContext;
	}

	/**
	 * Returns a specific application context associated to a Kurento handler
	 * servlet. This method returns null if the context does not exist.
	 * 
	 * @param servletClass
	 * @param ctx
	 * @return
	 */
	public static AnnotationConfigApplicationContext getKurentoServletApplicationContext(
			Class<?> servletClass, String servletName, ServletContext ctx) {
		Assert.notNull(ctx,
				"Cannot recover KurentoServletApplicationContext from a null ServletContext");
		Assert.notNull(servletClass,
				"Cannot recover KurentoServletApplicationContext from a null Servlet class");
		@SuppressWarnings("unchecked")
		ConcurrentHashMap<String, AnnotationConfigApplicationContext> childContexts = (ConcurrentHashMap<String, AnnotationConfigApplicationContext>) ctx
				.getAttribute(KurentoApplicationContextUtils.KURENTO_SERVLET_APPLICATION_CONTEXTS_ATT_NAME);
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

	public static AnnotationConfigApplicationContext createKurentoServletApplicationContext(
			Class<?> servletClass, String servletName, ServletContext ctx,
			String handlerClassName) {

		Assert.notNull(ctx,
				"Cannot create KurentoServletApplicationContext from a null ServletContext");
		Assert.notNull(servletClass,
				"Cannot create KurentoServletApplicationContext from a null Servlet class");
		Assert.notNull(servletClass,
				"Cannot create KurentoServletApplicationContext from a null Hanlder class");

		@SuppressWarnings("unchecked")
		ConcurrentHashMap<String, AnnotationConfigApplicationContext> childContexts = (ConcurrentHashMap<String, AnnotationConfigApplicationContext>) ctx
				.getAttribute(KurentoApplicationContextUtils.KURENTO_SERVLET_APPLICATION_CONTEXTS_ATT_NAME);
		if (childContexts == null) {
			childContexts = new ConcurrentHashMap<String, AnnotationConfigApplicationContext>();
		}

		AnnotationConfigApplicationContext childContext = childContexts
				.get(servletClass.getName() + ":" + servletName);
		Assert.isNull(childContext,
				"Pre-existing context found associated to servlet class "
						+ servletClass.getName() + " and servlet name "
						+ servletName);

		childContext = new AnnotationConfigApplicationContext();
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		// beanDefinition.setAutowireCandidate(true); TODO is this necessary?
		// beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		// TODO is this necessary?
		beanDefinition.setBeanClassName(handlerClassName);
		childContext.registerBeanDefinition(handlerClassName, beanDefinition);
		childContext.setParent(getKurentoApplicationContext(ctx));
		childContext.refresh();

		childContexts.put(servletClass.getName(), childContext);

		return childContext;
	}

	public static void closeAllKurentoApplicationContexts(ServletContext ctx) {
		Assert.notNull(ctx, "Cannot close contexts from a null ServletContext");

		// Close child Servlet contexts
		@SuppressWarnings("unchecked")
		ConcurrentHashMap<String, AnnotationConfigApplicationContext> childContexts = (ConcurrentHashMap<String, AnnotationConfigApplicationContext>) ctx
				.getAttribute(KurentoApplicationContextUtils.KURENTO_SERVLET_APPLICATION_CONTEXTS_ATT_NAME);
		if (childContexts != null) {
			for (AnnotationConfigApplicationContext childContext : childContexts
					.values()) {
				childContext.close();
			}
		}

		// Close Kurento parent context
		AnnotationConfigApplicationContext kurentoAppContext = (AnnotationConfigApplicationContext) ctx
				.getAttribute(KURENTO_APPLICATION_CONTEXT_ATT_NAME);
		if (kurentoAppContext != null) {
			kurentoAppContext.close();
		}
	}

	public static void processInjectionBasedOnApplicationContext(Object bean,
			AnnotationConfigApplicationContext appContext) {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(appContext.getAutowireCapableBeanFactory());
		bpp.processInjection(bean);
	}
}
