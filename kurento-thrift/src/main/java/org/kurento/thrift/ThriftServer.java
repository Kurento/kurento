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
package org.kurento.thrift;

import java.net.InetSocketAddress;

import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer.Args;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.kurento.thrift.internal.ThriftInterfaceExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server handler implementation for the thrift interface.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 *
 */
public class ThriftServer {

	private static final Logger log = LoggerFactory
			.getLogger(ThriftServer.class);

	private final TProcessor processor;

	private final InetSocketAddress addr;

	private final ThriftInterfaceExecutorService executorService;

	private TServer server;

	/**
	 * Default constructor
	 *
	 * @param processor
	 * @param executorService
	 * @param addr
	 */
	public ThriftServer(TProcessor processor,
			ThriftInterfaceExecutorService executorService,
			InetSocketAddress addr) {

		this.executorService = executorService;
		this.processor = processor;
		this.addr = addr;
	}

	/**
	 * Starts the thrift server in the configured address and port.
	 *
	 * @throws ThriftServerException
	 *             if the server can't bind to the provided address, or it
	 *             cannot be started
	 */
	public void start() {

		TNonblockingServerTransport transport;

		try {
			transport = new TNonblockingServerSocket(addr);
		} catch (TTransportException e) {
			throw new ThriftServerException(
					"Could not start media handler server on "
							+ addr.toString() + ". Reason: " + e.getMessage(),
					e);
		}

		log.debug("Thrift server started in {}", addr);

		// TODO default selectorThreads is 2. Test if this is enough under load,
		// or we would need more selector threads.
		Args args = new Args(transport);
		args.executorService(executorService.getExecutor())
				.processor(processor);

		server = new NonBlockingTThreadedSelectorServer(args);
		server.serve();
	}

	public synchronized void destroy() {
		if (server != null) {
			log.debug("Closing Thrift server at {}", addr);
			server.stop();
			log.debug("Thrift server closed", addr);
		}
	}

}
