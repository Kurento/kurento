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
package com.kurento.kmf.media.internal.pool;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.Client;

/**
 * Service that exposes the asynchronous and synchronous client pools.
 * 
 * @author Ivan Gracia
 * 
 */
public class MediaServerClientPoolService {

	@Autowired
	private MediaServerAsyncClientPool asyncClientPool;

	@Autowired
	private MediaServerSyncClientPool syncClientPool;

	public AsyncClient acquireAsync() throws PoolLimitException,
			KurentoMediaFrameworkException {
		return asyncClientPool.acquire();
	}

	public Client acquireSync() throws PoolLimitException,
			KurentoMediaFrameworkException {
		return syncClientPool.acquire();
	}

	public void release(AsyncClient client) {
		asyncClientPool.release(client);
	}

	public void release(Client client) {
		syncClientPool.release(client);
	}
}
