package com.kurento.kmf.media.internal.pool;

import com.kurento.kms.thrift.api.MediaServerService.Client;

class MediaServerSyncClientPool extends AbstractPool<Client> {

	MediaServerSyncClientPool() {
		super(new MediaServerSyncClientFactory());
	}
}
