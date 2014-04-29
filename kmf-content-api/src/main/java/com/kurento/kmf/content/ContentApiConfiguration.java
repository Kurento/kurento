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
package com.kurento.kmf.content;

/**
 * Configuration parameters for Content Management API.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public class ContentApiConfiguration {

	// Default values

	/**
	 * Minimal size of the thread pool executing callbacks on content Handlers
	 * (e.g. {@link HttpPlayerHander}, {@HttpRecorderHandler
	 * 
	 * 
	 * }, and so on). These threads will be kept in the
	 * pool, even if they are idle.
	 */
	private int poolCoreSize = 10;

	/**
	 * Maximum number of threads to allow in the thread pool.
	 */
	private int poolMaxSize = 100;

	/**
	 * Timeout (in milliseconds) that a Hanlder callback thread will wait before
	 * canceling the request and throwing and exception.
	 */
	private long poolExecutionTimeout = 50000L;

	/**
	 * Size of the queue used for holding tasks before they are executed in the
	 * thread pool.
	 */
	private int poolMaxQueueSize = 100;

	/**
	 * Timeout in milliseconds until a connection is established in the
	 * Streaming Proxy.
	 */
	private int proxyConnectionTimeout = 10000;

	/**
	 * Timeout for waiting for data or, put differently, a maximum period
	 * inactivity between two consecutive data packets. A timeout value of zero
	 * is interpreted as an infinite timeout.
	 */
	private int proxySocketTimeout = 0;

	/**
	 * Maximum total connections in Streaming Proxy.
	 */
	private int proxyMaxConnections = 1000;

	/**
	 * Maximum total connections in Streaming Proxy per route. In the context of
	 * the Streming Proxy, A 'route' is defined as the protocol (HTTP/HTTPS) +
	 * host + port.
	 */
	private int proxyMaxConnectionsPerRoute = 20;

	/**
	 * Timeout (in milliseconds) for Poll operation for WebRTC events.
	 */
	private long webRtcEventQueuePollTimeout = 15000L;

	/**
	 * Setter (mutator) for poolCoreSize field.
	 * 
	 * @param poolCoreSize
	 *            Minimal size of the Thread pool executing callbacks on content
	 *            Handlers. Default: 10
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
	 *            content Handlers. Default: 100
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
	 * @return Timeout (in milliseconds) that a Handler callback thread will
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
	 * Getter (accessor) for proxyConnectionTimeout field.
	 * 
	 * @return Timeout in milliseconds until a connection is established in the
	 *         Streaming Proxy
	 */
	public int getProxyConnectionTimeout() {
		return proxyConnectionTimeout;
	}

	/**
	 * Setter (mutator) for proxyConnectionTimeout field.
	 * 
	 * @param proxyConnectionTimeout
	 *            Timeout in milliseconds until a connection is established in
	 *            the Streaming Proxy. Default: 10000
	 */
	public void setProxyConnectionTimeout(int proxyConnectionTimeout) {
		this.proxyConnectionTimeout = proxyConnectionTimeout;
	}

	/**
	 * Getter (accessor) for proxySocketTimeout field.
	 * 
	 * @return Timeout for waiting for data in Streaming Proxy. Default: 0
	 *         (infinite timeout)
	 */
	public int getProxySocketTimeout() {
		return proxySocketTimeout;
	}

	/**
	 * Setter (mutator) for proxySocketTimeout field.
	 * 
	 * @param proxySocketTimeout
	 *            Timeout for waiting for data in Streaming Proxy
	 */
	public void setProxySocketTimeout(int proxySocketTimeout) {
		this.proxySocketTimeout = proxySocketTimeout;
	}

	/**
	 * Getter (accessor) for proxyMaxConnections field.
	 * 
	 * @return Maximum total connections in Streaming Proxy. Default: 1000
	 */
	public int getProxyMaxConnections() {
		return proxyMaxConnections;
	}

	/**
	 * Setter (mutator) for proxyMaxConnections field.
	 * 
	 * @param proxyMaxConnections
	 *            Maximum total connections in Streaming Proxy
	 */
	public void setProxyMaxConnections(int proxyMaxConnections) {
		this.proxyMaxConnections = proxyMaxConnections;
	}

	/**
	 * Getter (accessor) for proxyMaxConnectionsPerRoute field.
	 * 
	 * @return Maximum total connections in Streaming Proxy per route. In the
	 *         context of the Streming Proxy, A 'route' is defined as the
	 *         protocol (HTTP/HTTPS) + host + port. Default: 20
	 */
	public int getProxyMaxConnectionsPerRoute() {
		return proxyMaxConnectionsPerRoute;
	}

	/**
	 * Setter (mutator) for proxyMaxConnectionsPerRoute field.
	 * 
	 * @param proxyMaxConnectionsPerRoute
	 *            Maximum total connections in Streaming Proxy
	 */
	public void setProxyMaxConnectionsPerRoute(int proxyMaxConnectionsPerRoute) {
		this.proxyMaxConnectionsPerRoute = proxyMaxConnectionsPerRoute;
	}

	/**
	 * Getter (accessor) for webRtcEventQueuePollTimeout field.
	 * 
	 * @return Timeout (in milliseconds) for Poll operation for WebRTC events
	 */
	public long getWebRtcEventQueuePollTimeout() {
		return webRtcEventQueuePollTimeout;
	}

	/**
	 * Setter (mutator) for webRtcEventQueuePollTimeout field.
	 * 
	 * @param webRtcEventQueuePollTimeout
	 *            Timeout (in milliseconds) for Poll operation for WebRTC
	 *            events. Default: 15000
	 */
	public void setWebRtcEventQueuePollTimeout(long webRtcEventQueuePollTimeout) {
		this.webRtcEventQueuePollTimeout = webRtcEventQueuePollTimeout;
	}

}
