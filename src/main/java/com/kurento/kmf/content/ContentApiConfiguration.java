package com.kurento.kmf.content;

public class ContentApiConfiguration {

	// Default values
	private int poolCoreSize = 10;
	private long poolExecutionTimeout = 50000L;
	private int poolMaxSize = 100;
	private int poolMaxQueueSize = 100;
	private int proxyConnectionTimeout = 10000;
	private int proxySocketTimeout = 0;
	private int proxyMaxConnections = 1000;
	private int proxyMaxConnectionsPerRoute = 2;

	/**
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
	 * 
	 * @param poolMaxQueueSize
	 *            Maximum size of the waiting queue of the Thread pool executing
	 *            callbacks. Threads wait in this queue up to
	 *            poolExecutionTimeout. Default: 100
	 */
	public void setPoolMaxQueueSize(int poolMaxQueueSize) {
		this.poolMaxQueueSize = poolMaxQueueSize;
	}

	public int getPoolMaxQueueSize() {
		return poolMaxQueueSize;
	}

	public int getPoolCoreSize() {
		return poolCoreSize;
	}

	public long getPoolExecutionTimeout() {
		return poolExecutionTimeout;
	}

	public int getPoolMaxSize() {
		return poolMaxSize;
	}

	public int getProxyConnectionTimeout() {
		return proxyConnectionTimeout;
	}

	public void setProxyConnectionTimeout(int proxyConnectionTimeout) {
		this.proxyConnectionTimeout = proxyConnectionTimeout;
	}

	public int getProxySocketTimeout() {
		return proxySocketTimeout;
	}

	public void setProxySocketTimeout(int proxySocketTimeout) {
		this.proxySocketTimeout = proxySocketTimeout;
	}

	public int getProxyMaxConnections() {
		return proxyMaxConnections;
	}

	public void setProxyMaxConnections(int proxyMaxConnections) {
		this.proxyMaxConnections = proxyMaxConnections;
	}

	public int getProxyMaxConnectionsPerRoute() {
		return proxyMaxConnectionsPerRoute;
	}

	public void setProxyMaxConnectionsPerRoute(int proxyMaxConnectionsPerRoute) {
		this.proxyMaxConnectionsPerRoute = proxyMaxConnectionsPerRoute;
	}

}
