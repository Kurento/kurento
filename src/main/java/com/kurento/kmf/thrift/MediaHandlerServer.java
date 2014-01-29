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
package com.kurento.kmf.thrift;

import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.server.TThreadedSelectorServer.Args;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;

/**
 * Server handler implementation for the thrift interface. This handler is used
 * by KMS to send events and error to the media-api
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 * 
 */
public class MediaHandlerServer {

	private static final Logger log = LoggerFactory
			.getLogger(MediaHandlerServer.class.getName());

	private final Processor<?> processor;

	private final InetSocketAddress addr;

	/**
	 * Media-API global executor service
	 */
	@Autowired
	private ThriftInterfaceExecutorService executorService;

	private TServer server;

	/**
	 * Default constructor
	 * 
	 * @param processor
	 * @param addr
	 */
	public MediaHandlerServer(Processor<?> processor, InetSocketAddress addr) {
		this.processor = processor;
		this.addr = addr;
	}

	@PostConstruct
	private void init() {
		TNonblockingServerTransport transport;
		try {
			transport = new TNonblockingServerSocket(addr);
		} catch (TTransportException e) {
			throw new KurentoMediaFrameworkException(
					"Could not start media handler server. " + e.getMessage(),
					e, 30003);
		}

		// TODO default selectorThreads is 2. Test if this is enough under load,
		// or we would need more selector threads.
		Args args = new Args(transport);
		args.executorService(executorService.getExecutor())
				.processor(processor);

		server = new NonBlockingTThreadedSelectorServer(args);
		server.serve();
	}

	@PreDestroy
	private synchronized void destroy() {
		if (server != null) {
			server.stop();
		}
	}

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
			log.debug("Starting");
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
