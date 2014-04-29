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
package com.kurento.kmf.thrift.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

/**
 * Service that exposes the asynchronous and synchronous client pools.
 * 
 * @author Ivan Gracia
 * 
 */
public class MediaServerClientPoolService {

	private static final Logger log = LoggerFactory
			.getLogger(MediaServerClientPoolService.class);

	@Autowired
	private MediaServerAsyncClientPool asyncClientPool;

	@Autowired
	private MediaServerSyncClientPool syncClientPool;

	// Used in Spring environments
	public MediaServerClientPoolService() {
	}

	// Used in non Spring environments
	public MediaServerClientPoolService(
			MediaServerAsyncClientPool asyncClientPool,
			MediaServerSyncClientPool syncClientPool) {

		this.asyncClientPool = asyncClientPool;
		this.syncClientPool = syncClientPool;
	}

	public AsyncClient acquireAsync() throws PoolLimitException,
			KurentoMediaFrameworkException {
		log.trace("Acquiring async client from pool");
		return asyncClientPool.acquire();
	}

	public Client acquireSync() throws PoolLimitException,
			KurentoMediaFrameworkException {
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
