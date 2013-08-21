package com.kurento.kmf.content.internal;

import java.util.concurrent.ConcurrentHashMap;

import com.kurento.kmf.content.internal.base.AbstractContentRequest;

public class ContentRequestManager {

	private ConcurrentHashMap<String, AbstractContentRequest> requests = new ConcurrentHashMap<String, AbstractContentRequest>();

	public ContentRequestManager() {
	}

	public AbstractContentRequest put(AbstractContentRequest contentRequest) {
		return requests.put(contentRequest.getSessionId(), contentRequest);
	}

	public AbstractContentRequest get(String requestId) {
		return requests.get(requestId);
	}

	public AbstractContentRequest remove(String requestId) {
		return requests.remove(requestId);
	}
}
