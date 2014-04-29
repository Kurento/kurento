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
