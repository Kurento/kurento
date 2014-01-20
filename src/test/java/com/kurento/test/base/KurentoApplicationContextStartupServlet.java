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
package com.kurento.test.base;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * Servlet which initializes Kurento Application Context.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
@WebServlet(loadOnStartup = 1, urlPatterns = { "/testit" })
public class KurentoApplicationContextStartupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		super.init();
		KurentoApplicationContextUtils.createKurentoApplicationContext(this
				.getServletContext());
	}

}
