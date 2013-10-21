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
package com.kurento.kmf.media;

import com.kurento.kmf.media.internal.pool.AbstractPool;

/**
 * Configuration parameters for Media API.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Iván Gracia (igracia@gsyc.es)
 * @version 2.0.0
 */
public class MediaApiConfiguration {
	/**
	 * Address where the thrift server exposed by the Kurento Media Server is
	 * listening.
	 */
	private String serverAddress = "localhost";

	/**
	 * Port of the Kurento Media Server thrift server.
	 */
	private int serverPort = 9090;

	/**
	 * Address of the local thrift server, which will be used to receive events
	 * and error notifications sent by the Kurento Media Server.
	 */
	private String handlerAddress = "localhost";

	/**
	 * Port where the local thrift server will be listening.
	 */
	private int handlerPort = 9191;

	// Default values

	/**
	 * Minimal size of the thread pool serving requests from the thrift server.
	 * These threads will be kept in the pool, even if they are idle.
	 */
	private int poolCoreSize = 10;

	/**
	 * Maximum number of threads to allow in the thread pool.
	 */
	private int poolMaxSize = 100;

	/**
	 * Timeout (in milliseconds) that a Hanlder callback thread will wait before
	 * cancelling the request and throwing and exception.
	 */
	private long poolExecutionTimeout = 50000L;

	/**
	 * Size of the queue used for holding tasks before they are executed in the
	 * thread pool.
	 */
	private int poolMaxQueueSize = 100;

	/**
	 * Size of the pool of thrift clients. Each pool created by the
	 * {@link AbstractPool} will be instantiated with this number of clients.
	 */
	private int clientPoolSize = 5;

	/**
	 * Getter (accessor) for serverAddress field.
	 * 
	 * @return Address where the thrift server exposed by the Kurento Media
	 *         Server is listening.
	 */
	public String getServerAddress() {
		return serverAddress;
	}

	/**
	 * Setter (mutator) for serverAddress field.
	 * 
	 * @param serverAddress
	 *            Address where the thrift server exposed by the Kurento Media
	 *            Server is listening.
	 */
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Getter (accessor) for serverPort field.
	 * 
	 * @return Port of the Kurento Media Server thrift server.
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Setter (mutator) for serverPort field.
	 * 
	 * @param serverPort
	 *            Port of the Kurento Media Server thrift server.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Getter (accessor) for handlerAddress field.
	 * 
	 * @return Address of the local thrift server, which will be used to receive
	 *         events and error notifications sent by the Kurento Media Server.
	 */
	public String getHandlerAddress() {
		return handlerAddress;
	}

	/**
	 * Setter (mutator) for handlerAddress field.
	 * 
	 * @param handlerAddress
	 *            Address of the local thrift server, which will be used to
	 *            receive events and error notifications sent by the Kurento
	 *            Media Server.
	 */
	public void setHandlerAddress(String handlerAddress) {
		this.handlerAddress = handlerAddress;
	}

	/**
	 * Getter (accessor) for handlerPort field.
	 * 
	 * @return Port where the local thrift server will be listening.
	 */
	public int getHandlerPort() {
		return handlerPort;
	}

	/**
	 * Setter (mutator) for handlerPort field.
	 * 
	 * @param handlerPort
	 *            Port where the local thrift server will be listening.
	 */
	public void setHandlerPort(int handlerPort) {
		this.handlerPort = handlerPort;
	}

	/**
	 * Setter (mutator) for poolCoreSize field.
	 * 
	 * @param poolCoreSize
	 *            Minimal size of the Thread pool executing callbacks on content
	 *            Handlers (e.g. PlayerHander, RecorderHandler, etc.) Default:
	 *            10
	 */
	public void setPoolCoreSize(int poolCoreSize) {
		this.poolCoreSize = poolCoreSize;
	}

	/**
	 * Setter (mutator) for poolExecutionTimeout field.
	 * 
	 * @param poolExecutionTimeout
	 *            timeout (in milliseconds) that a Hanlder callback Thread will
	 *            wait before canceling the request and throwing and exception.
	 *            Default: 50000
	 */
	public void setPoolExecutionTimeout(long poolExecutionTimeout) {
		this.poolExecutionTimeout = poolExecutionTimeout;
	}

	/**
	 * Setter (mutator) for poolMaxSize field.
	 * 
	 * @param poolMaxSize
	 *            Maximum size of the of the Thread pool executing callbacks on
	 *            content Handlers (e.g. PlayerHander, RecorderHandler, etc.)
	 *            Default: 100
	 */
	public void setPoolMaxSize(int poolMaxSize) {
		this.poolMaxSize = poolMaxSize;
	}

	/**
	 * Setter (mutator) for poolMaxQueueSize field.
	 * 
	 * @param poolMaxQueueSize
	 *            Maximum size of the waiting queue of the Thread pool executing
	 *            callbacks. Threads wait in this queue up to
	 *            poolExecutionTimeout. Default: 100
	 */
	public void setPoolMaxQueueSize(int poolMaxQueueSize) {
		this.poolMaxQueueSize = poolMaxQueueSize;
	}

	/**
	 * Getter (accessor) for poolMaxQueueSize field.
	 * 
	 * @return Maximum size of the waiting queue of the Thread pool executing
	 *         callbacks
	 */
	public int getPoolMaxQueueSize() {
		return poolMaxQueueSize;
	}

	/**
	 * Getter (accessor) for poolCoreSize field.
	 * 
	 * @return Minimal size of the Thread pool
	 */
	public int getPoolCoreSize() {
		return poolCoreSize;
	}

	/**
	 * Getter (accessor) for poolExecutionTimeout field.
	 * 
	 * @return Timeout (in milliseconds) that a Hanlder callback thread will
	 *         wait
	 */
	public long getPoolExecutionTimeout() {
		return poolExecutionTimeout;
	}

	/**
	 * Getter (accessor) for poolMaxSize field.
	 * 
	 * @return Maximum size of the of the Thread pool executing callbacks on
	 *         content Handlers
	 */
	public int getPoolMaxSize() {
		return poolMaxSize;
	}

	/**
	 * Getter (accessor) for clientPoolSize field.
	 * 
	 * @return Size of the pool of thrift clients. Each pool created by the
	 *         {@link AbstractPool} will be instantiated with this number of
	 *         clients.
	 */
	public int getClientPoolSize() {
		return clientPoolSize;
	}

	/**
	 * Setter (mutator) for clientPoolSize field.
	 * 
	 * @param clientPoolSize
	 *            Size of the pool of thrift clients. Each pool created by the
	 *            {@link AbstractPool} will be instantiated with this number of
	 *            clients.
	 */
	public void setClientPoolSize(int clientPoolSize) {
		this.clientPoolSize = clientPoolSize;
	}

}
