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
package com.kurento.kmf.media.internal;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.thrift.TException;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaHandlerService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;

public class MediaHandlerServer {

	@Autowired
	private MediaApiConfiguration config;

	@Autowired
	private MediaPipelineFactory mediaPipelineFactory;

	@Autowired
	private MediaServerCallbackHandler handler;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private TaskExecutor taskExecutor;

	public MediaHandlerServer() {
	}

	@PostConstruct
	private void init() {
		start();
	}

	@PreDestroy
	private synchronized void destroy() {
		stop();
	}

	private synchronized void start() {
		try {
			TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
					config.getHandlerPort());
			// TODO maybe TThreadedSelectorServer or other type of server
			TServer server = new TNonblockingServer(
					new TNonblockingServer.Args(serverTransport)
							.processor(processor));
			taskExecutor.execute(new ServerTask(server));
		} catch (TTransportException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(
					"Could not start media handler server", e, 30000);
		}
	}

	private synchronized void stop() {
		TServer server = ServerTask.server;

		if (server != null) {
			server.stop();
			server = null;
		}
	}

	private static class ServerTask implements Runnable {

		private static TServer server;

		public ServerTask(TServer tServer) {
			server = tServer;
		}

		@Override
		public void run() {
			server.serve();
		}
	}

	private final Processor<KmsMediaHandlerService.Iface> processor = new Processor<Iface>(
			new Iface() {

				@Override
				public void onError(String callbackToken, KmsMediaError error)
						throws TException {
					MediaError mediaError = (MediaError) applicationContext
							.getBean("mediaError", error);
					ErrorListenerRegistration registration = new ErrorListenerRegistration(
							callbackToken);
					handler.onError(registration,
							Long.valueOf(error.getSource().id), mediaError);
				}

				@Override
				public void onEvent(String callbackToken, KmsMediaEvent event)
						throws TException {
					MediaEvent mediaEvent = (MediaEvent) applicationContext
							.getBean("mediaEvent", event);

					EventListenerRegistration registration = new EventListenerRegistration(
							callbackToken);
					handler.onEvent(registration,
							Long.valueOf(event.getSource().id), mediaEvent);
				}
			});
}
