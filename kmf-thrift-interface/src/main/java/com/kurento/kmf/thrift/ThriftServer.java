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

import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer.Args;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;

/**
 * Server handler implementation for the thrift interface. This handler is used
 * by KMS to send events and error to the media-api
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.0
 * 
 */
public class ThriftServer {

	private static Logger LOG = LoggerFactory.getLogger(ThriftServer.class);

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

		LOG.info("Configuring thrift server on {}", addr);

		this.executorService = executorService;
		this.processor = processor;
		this.addr = addr;
	}

	public void start() {

		LOG.info("Starting thrift server at {}", addr);

		TNonblockingServerTransport transport;

		try {
			transport = new TNonblockingServerSocket(addr);
		} catch (TTransportException e) {
			throw new KurentoMediaFrameworkException(
					"Could not start media handler server on "
							+ addr.toString() + "\n Reason: " + e.getMessage(),
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

	public synchronized void destroy() {
		if (server != null) {
			server.stop();
		}
	}

}
