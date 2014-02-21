/*
v  * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.kmf.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

/**
 * 
 * Defines the events associated to the handler entity.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public abstract class ContentHandler<T extends ContentSession> {

	private static final Logger log = LoggerFactory
			.getLogger(ContentHandler.class);

	/**
	 * Invoked by the framework when a request is received from a client for
	 * sending/receiving content
	 * 
	 * @param contentSession
	 *            session associated to the request automatically created by the
	 *            framework
	 * @throws Exception
	 *             possible exception thrown by handler implementation
	 */
	public abstract void onContentRequest(T contentSession) throws Exception;

	/**
	 * Invoked by the framework when the content exchange really starts. For
	 * example, in an HttpPlayerService, this methods is invoked when the
	 * HttpEndpoint really receives the media request from the client
	 * 
	 * @param contentSession
	 *            the session for which the content exchange started
	 * @throws Exception
	 *             possible exception thrown by handler implementation
	 */
	public void onContentStarted(T contentSession) throws Exception {
		getLogger().info(
				"Call to default onContentStarted in session "
						+ contentSession.getContentId());
	}

	/**
	 * Invoked by the framework when a ContentSession successfully concludes.
	 * This method is invoked if the end-client request and explicit termination
	 * of the session.
	 * 
	 * @param contentSession
	 *            the ContentSession completing its job
	 * @throws Exception
	 *             possible exception thrown by handler implementation
	 */
	public void onSessionTerminated(T contentSession, int code, String reason)
			throws Exception {
		getLogger().info(
				"Call to default onSessionTerminated in session "
						+ contentSession.getContentId());
	}

	/**
	 * Invoked by the framework when a ContentSession receives a user defined
	 * command from the client.
	 * 
	 * @param contentSession
	 *            The ContentSession receiving the command
	 * @param contentCommand
	 *            The command received from client
	 * @throws Exception
	 *             possible exception thrown by handler implementation
	 */
	public ContentCommandResult onContentCommand(T contentSession,
			ContentCommand contentCommand) throws Exception {
		throw new KurentoMediaFrameworkException(
				"Handler must implement onContentCommand for being able to receive commands;",
				10026);
	}

	/**
	 * Invoked by the framework when a session enters into an error condition
	 * due to an asynchronous media server error. The invocation of this method
	 * always implies the invalidation of the session and the conclusion of the
	 * content exchange. This method is not invoked if the content session is
	 * explicitly terminated by invoking its terminate method.
	 * 
	 * @param contentSession
	 *            The ContentSession receiving the error
	 * @param code
	 *            The return code coming from the Media Server
	 * @param description
	 *            A description of the error.
	 * @throws Exception
	 */
	public void onSessionError(T contentSession, int code, String description)
			throws Exception {
		log.error(description);
	}

	/**
	 * This method is invoked every time the handler implementation throws and
	 * exception which is not caught by the handler itself. In other words, when
	 * an exception is thrown to the Content API framework by the handler. It is
	 * important to remark that if this method throws an exception itself, it
	 * will produce a recursive call which may drive to abnormal program
	 * termination. The invocation of this method does not imply the
	 * invalidation of the session.
	 * 
	 * TODO: explain what the default implementation makes
	 * 
	 * @param contentSession
	 * @param exception
	 * @throws Exception
	 */
	public void onUncaughtException(T contentSession, Throwable exception)
			throws Exception {
		log.error(exception.getMessage(), exception);
		int code = 9999;
		if (exception instanceof KurentoMediaFrameworkException) {
			code = ((KurentoMediaFrameworkException) exception).getCode();
		}
		contentSession.terminate(code, exception.getMessage());
	}

	protected Logger getLogger() {
		return log;
	}
}
