package com.kurento.kmf.content.internal;

import java.util.concurrent.ConcurrentHashMap;

import com.kurento.kmf.content.internal.base.AbstractContentRequest;

/**
 * 
 * Concurrent hash map implementation for the content request (stored using the
 * session id as key).
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class ContentRequestManager {

	/**
	 * Map to store the content requests.
	 */
	private ConcurrentHashMap<String, AbstractContentRequest> requests = new ConcurrentHashMap<String, AbstractContentRequest>();

	/**
	 * Default constructor.
	 */
	public ContentRequestManager() {
	}

	/**
	 * Mutator for content request concurrent hash map.
	 * 
	 * @param contentRequest
	 *            Content request to be stored in the concurrent hash map
	 * @return stored content request
	 */
	public AbstractContentRequest put(AbstractContentRequest contentRequest) {
		return requests.put(contentRequest.getSessionId(), contentRequest);
	}

	/**
	 * Accessor (getter) for the concurrent hash map.
	 * 
	 * @param requestId
	 *            key in the map (contentRequest.sessionId)
	 * @return found content request
	 */
	public AbstractContentRequest get(String requestId) {
		return requests.get(requestId);
	}

	/**
	 * Remove an element of the concurrent hash map.
	 * 
	 * @param requestId
	 *            key in the map (contentRequest.sessionId)
	 * @return deleted content request
	 */
	public AbstractContentRequest remove(String requestId) {
		return requests.remove(requestId);
	}
}
