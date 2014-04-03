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

import com.kurento.kmf.thrift.pool.AbstractPool;

/**
 * Configuration parameters for Media API. This class is intended to be created
 * as a bean inside an Spring context, and is needed by the Media API to work
 * correctly.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Iván Gracia (igracia@gsyc.es)
 * @version 2.0.0
 */
public class ThriftInterfaceConfiguration {

	/**
	 * Address where the thrift server exposed by the Kurento Media Server is
	 * listening.
	 */
	private String serverAddress = "localhost";

	/**
	 * Port of the Kurento Media Server thrift server.
	 */
	private int serverPort = 9494;

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
	 * Size of the pool of thrift clients. Each pool created by the abstract
	 * pool will be instantiated with this number of clients.
	 */
	private int clientPoolSize = 5;

	// Used in Spring environments
	public ThriftInterfaceConfiguration() {
	}

	// Used in non Spring environments
	public ThriftInterfaceConfiguration(String serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
	}

	/**
	 * Obtains the address where the thrift server exposed by the Kurento Media
	 * Server is listening.
	 * 
	 * @return The address.
	 */
	public String getServerAddress() {
		return serverAddress;
	}

	/**
	 * Sets the address where the thrift server exposed by the Kurento Media
	 * Server is listening.
	 * 
	 * @param serverAddress
	 *            The address to set.
	 * 
	 */
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Gets the port of the Kurento Media Server thrift server
	 * 
	 * @return The port.
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Sets the port of the Kurento Media Server thrift server
	 * 
	 * @param serverPort
	 *            The port.
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Gets the minimal size of the Thread pool executing callbacks on listeners
	 * Default: 10
	 * 
	 * @return The configured pool size.
	 */
	public int getPoolCoreSize() {
		return poolCoreSize;
	}

	/**
	 * Sets the minimal size of the Thread pool executing callbacks on listeners
	 * Default: 10
	 * 
	 * @param poolCoreSize
	 *            Size of the pool.
	 */
	public void setPoolCoreSize(int poolCoreSize) {
		this.poolCoreSize = poolCoreSize;
	}

	/**
	 * Gets the time (in milliseconds) that a listener callback Thread will wait
	 * before cancelling the request and throwing and exception. Default: 50000
	 * 
	 * @return The time in milliseconds.
	 */
	public long getPoolExecutionTimeout() {
		return poolExecutionTimeout;
	}

	/**
	 * Sets the time (in milliseconds) that a listener callback Thread will wait
	 * before cancelling the request and throwing and exception. Default: 50000
	 * 
	 * @param poolExecutionTimeout
	 *            Time in milliseconds.
	 */
	public void setPoolExecutionTimeout(long poolExecutionTimeout) {
		this.poolExecutionTimeout = poolExecutionTimeout;
	}

	/**
	 * Gets the maximum number of threads to allow in the pool.
	 * 
	 * @return The maximum number of threads in the pool.
	 */
	public int getPoolMaxSize() {
		return poolMaxSize;
	}

	/**
	 * Sets the maximum number of threads to allow in the pool.
	 * 
	 * @param poolMaxSize
	 *            The threads to be allowed in the pool.
	 */
	public void setPoolMaxSize(int poolMaxSize) {
		this.poolMaxSize = poolMaxSize;
	}

	/**
	 * Gets the number of threads that can be waiting to be processed. In the
	 * context of the Media API, each thread represents a thrift connection
	 * waiting to be processed. Threads wait in this queue up to
	 * {@code poolExecutionTimeout}. Default: 100
	 * 
	 * @return The size of the waiting queue
	 */
	public int getPoolMaxQueueSize() {
		return poolMaxQueueSize;
	}

	/**
	 * Sets the number of threads that can be waiting to be processed. In the
	 * context of the Media API, each thread represents a thrift connection
	 * waiting to be processed. Threads wait in this queue up to
	 * {@code poolExecutionTimeout}. Default: 100
	 * 
	 * @param poolMaxQueueSize
	 *            The maximum number of waiting threads.
	 */
	public void setPoolMaxQueueSize(int poolMaxQueueSize) {
		this.poolMaxQueueSize = poolMaxQueueSize;
	}

	/**
	 * Gets the size of the pool of thrift clients. Each pool created by the
	 * {@link AbstractPool} will be instantiated with this number of clients.
	 * 
	 * @return The size of the pool.
	 */
	public int getClientPoolSize() {
		return clientPoolSize;
	}

	/**
	 * Sets the size of the pool of thrift clients. Each pool created by the
	 * {@link AbstractPool} will be instantiated with this number of clients.
	 * 
	 * @param clientPoolSize
	 *            The size of the client pool.
	 */
	public void setClientPoolSize(int clientPoolSize) {
		this.clientPoolSize = clientPoolSize;
	}

}
