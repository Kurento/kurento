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

import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.server.TThreadedSelectorServer.Args;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaHandlerService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Iface;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;

/**
 * Server handler implementation for the thrift interface. This handler is used
 * by KMS to send events and error to the media-api
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * 
 */
public class MediaHandlerServer {

	private static final Logger log = LoggerFactory
			.getLogger(MediaHandlerServer.class.getName());

	/**
	 * Autowired configuration.
	 */
	@Autowired
	private MediaApiConfiguration config;

	/**
	 * Callback handler to be invoked when receiving error and event
	 * notifications from the KMS
	 */
	@Autowired
	private MediaServerCallbackHandler handler;

	/**
	 * Spring context, used to instantiate {@link MediaError} and
	 * {@link MediaEvent} objects
	 */
	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * Media-API global executor service
	 */
	@Autowired
	private MediaApiExecutorService executorService;

	private TServer server;

	/**
	 * Default constructor
	 */
	public MediaHandlerServer() {
	}

	@PostConstruct
	private void init() {
		start();
	}

	@PreDestroy
	private synchronized void destroy() {
		if (server != null) {
			server.stop();
		}
	}

	private synchronized void start() {

		TNonblockingServerTransport transport;
		try {
			transport = new TNonblockingServerSocket(new InetSocketAddress(
					config.getHandlerAddress(), config.getHandlerPort()));
		} catch (TTransportException e) {
			throw new KurentoMediaFrameworkException(
					"Could not start media handler server", e, 30003);
		}

		// TODO default selectorThreads is 2. Test if this is enough under load,
		// or we would need more selector threads.
		Args args = new Args(transport);
		args.executorService(executorService.getExecutor())
				.processor(processor);

		server = new NonBlockingTThreadedSelectorServer(args);
		server.serve();
	}

	private final Processor<KmsMediaHandlerService.Iface> processor = new Processor<Iface>(
			new Iface() {

				@Override
				public void onError(String callbackToken, KmsMediaError error)
						throws TException {
					log.trace("KMS error {} received on object {}",
							Integer.toString(error.errorCode),
							Long.toString(error.getSource().getId()));
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
					log.trace("KMS event {} received on object {}", event.type,
							Long.toString(event.getSource().getId()));
					MediaEvent mediaEvent = (MediaEvent) applicationContext
							.getBean("mediaEvent", event);

					EventListenerRegistration registration = new EventListenerRegistration(
							callbackToken);
					handler.onEvent(registration,
							Long.valueOf(event.getSource().id), mediaEvent);
				}
			});

	/**
	 * Decorator of the {@link TThreadedSelectorServer}. This server does not
	 * block when invoking the {@link TServer#serve} method. The
	 * {@code ExecutorService} will not be closed by the server, and it is a
	 * task of the developer to gracefully shut down the service passed to the
	 * server
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * 
	 */
	private static class NonBlockingTThreadedSelectorServer extends
			TThreadedSelectorServer {

		/**
		 * @param args
		 */
		public NonBlockingTThreadedSelectorServer(Args args) {
			super(args);
		}

		/**
		 * Override of the {@link TThreadedSelectorServer#serve} method. This is
		 * a non-blocking call, achieved by skipping the call to
		 * {@code waitForShutdown()}.
		 */
		@Override
		public void serve() {
			// start any IO threads
			if (!startThreads()) {
				throw new KurentoMediaFrameworkException(
						"Could not start thread in Thrift server", 30001);
			}
			// start listening, or exit
			if (!startListening()) {
				throw new KurentoMediaFrameworkException(
						"Could not start listening in Thrift server", 30002);
			}
			setServing(true);
		}

		@Override
		public void stop() {
			setServing(false);
			super.stop();
		}
	}
}
