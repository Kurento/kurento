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
package org.kurento.thrift.pool;

import org.kurento.common.exception.KurentoException;
import org.kurento.thrift.ThriftInterfaceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

/**
 * Service that exposes the asynchronous and synchronous client pools.
 *
 * @author Ivan Gracia
 *
 */
public class ThriftClientPoolService {

	private static final Logger log = LoggerFactory
			.getLogger(ThriftClientPoolService.class);

	@Autowired
	private ThriftAsyncClientPool asyncClientPool;

	@Autowired
	private ThriftSyncClientPool syncClientPool;

	/**
	 * Default constructor, to be used in spring environments
	 */
	public ThriftClientPoolService() {
	}

	/**
	 * Constructor for non-spring environments.
	 *
	 * @param asyncClientPool
	 * @param syncClientPool
	 */
	public ThriftClientPoolService(ThriftAsyncClientPool asyncClientPool,
			ThriftSyncClientPool syncClientPool) {

		this.asyncClientPool = asyncClientPool;
		this.syncClientPool = syncClientPool;
	}

	public ThriftClientPoolService(ThriftInterfaceConfiguration config) {

		asyncClientPool = new ThriftAsyncClientPool(
				new ThriftAsyncClientFactory(config), config);

		syncClientPool = new ThriftSyncClientPool(new ThriftSyncClientFactory(
				config), config);
	}

	public AsyncClient acquireAsync() throws PoolLimitException,
			ClientPoolException, KurentoException {
		log.trace("Acquiring async client from pool");
		return asyncClientPool.acquire();
	}

	public Client acquireSync() throws PoolLimitException, ClientPoolException,
			KurentoException {
		log.trace("Acquiring sync client from pool");
		return syncClientPool.acquire();
	}

	public void release(AsyncClient client) {
		log.trace("Releasing async client from pool");
		asyncClientPool.release(client);
	}

	public void release(Client client) {
		log.trace("Releasing sync client from pool");
		syncClientPool.release(client);
	}
}
