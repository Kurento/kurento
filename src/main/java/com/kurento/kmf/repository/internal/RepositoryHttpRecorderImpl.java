package com.kurento.kmf.repository.internal;

import com.kurento.kmf.repository.RepositoryHttpRecorder;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.internal.http.RepositoryHttpManager;

public class RepositoryHttpRecorderImpl extends RepositoryHttpEndpointImpl
		implements RepositoryHttpRecorder {

	public RepositoryHttpRecorderImpl(RepositoryItem repositoryItem, String id,
			String url, RepositoryHttpManager httpManager) {
		super(repositoryItem, id, url, httpManager);
	}

}
