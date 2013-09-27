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
