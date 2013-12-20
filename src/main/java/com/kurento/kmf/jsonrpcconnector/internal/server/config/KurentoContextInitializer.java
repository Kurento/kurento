package com.kurento.kmf.jsonrpcconnector.internal.server.config;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.kurento.kmf.spring.KurentoApplicationContextUtils;

@Component
public class KurentoContextInitializer implements ServletContextAware {

	@Override
	public void setServletContext(ServletContext servletContext) {

		AnnotationConfigApplicationContext appCtx = KurentoApplicationContextUtils
				.getKurentoApplicationContext();

		if (appCtx == null) {
			appCtx = KurentoApplicationContextUtils
					.createKurentoApplicationContext(servletContext);
		}
	}

}
