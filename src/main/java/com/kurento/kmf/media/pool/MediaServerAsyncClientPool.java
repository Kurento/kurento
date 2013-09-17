package com.kurento.kmf.media.pool;

import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;

class MediaServerAsyncClientPool extends AbstractPool<AsyncClient> {

	MediaServerAsyncClientPool() {
		super(new MediaServerAsyncClientFactory());
	}
}
