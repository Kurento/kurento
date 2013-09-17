package com.kurento.kmf.media.pool;

import com.kurento.kms.thrift.api.MediaServerService.Client;

class MediaServerSyncClientPool extends AbstractPool<Client> {

	MediaServerSyncClientPool() {
		super(new MediaServerSyncClientFactory());
	}
}
