package com.kurento.kmf.repository.internal;

import com.kurento.kmf.repository.RepositoryHttpPlayer;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.internal.http.RepositoryHttpManager;

public class RepositoryHttpPlayerImpl extends RepositoryHttpEndpointImpl
		implements RepositoryHttpPlayer {

	public RepositoryHttpPlayerImpl(RepositoryItem repositoryItem, String id,
			String url, RepositoryHttpManager httpManager) {
		super(repositoryItem, id, url, httpManager);
	}

}
