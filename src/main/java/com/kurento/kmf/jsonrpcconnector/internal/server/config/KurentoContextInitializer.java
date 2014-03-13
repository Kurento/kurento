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

		KurentoApplicationContextUtils
				.processInjectionBasedOnKurentoApplicationContext(this);
	}

}
