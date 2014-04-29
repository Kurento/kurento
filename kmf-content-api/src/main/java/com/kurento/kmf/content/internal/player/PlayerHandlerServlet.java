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
package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.internal.base.AbstractContentHandlerServlet;
import com.kurento.kmf.content.internal.base.AbstractContentSession;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

/**
 * 
 * Handler servlet for Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class PlayerHandlerServlet extends AbstractContentHandlerServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(PlayerHandlerServlet.class);

	/**
	 * Look for {@link HttpPlayerService} annotation in the handler class and
	 * check whether or not it is using JSON control protocol.
	 * 
	 * @return JSON Control Protocol strategy (true|false)
	 */
	@Override
	protected boolean getUseJsonControlProtocol(Class<?> handlerClass)
			throws ServletException {
		return handlerClass.getAnnotation(HttpPlayerService.class)
				.useControlProtocol();
	}

	/**
	 * Create a content Request instance (as a Spring Bean).
	 * 
	 * @param asyncCtx
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 * @return Content Request
	 */
	@Override
	protected AbstractContentSession createContentSession(
			AsyncContext asyncCtx, String contentId) {
		return (HttpPlayerSessionImpl) KurentoApplicationContextUtils.getBean(
				"httpPlayerSessionImpl", handler, contentSessionManager,
				asyncCtx, contentId,
				handlerClass.getAnnotation(HttpPlayerService.class).redirect(),
				useControlProtocol);
	}

	/**
	 * Logger accessor (getter).
	 * 
	 * @return Logger
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}
}
