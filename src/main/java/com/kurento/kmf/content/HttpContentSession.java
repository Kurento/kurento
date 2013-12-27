package com.kurento.kmf.content;

import com.kurento.kmf.media.HttpEndpoint;
import com.kurento.kmf.repository.RepositoryItem;

public interface HttpContentSession extends ContentSession {

	/**
	 * Starts the content exchange for a given content path. TODO: What is the
	 * content path? TODO: Explain what starts mean
	 * 
	 * TODO: IN ALL STARTS OF ALL CONTENTSESSIONS. Explain that if starts throws
	 * and exception, then the session is invalidated. If you don't manage this
	 * exception, it will end-up in onUnmanagedException method of the handler,
	 * but the session will be terminated there.
	 * 
	 * @param contentPath
	 *            Identifies the content in a meaningful way for the Media
	 *            Server
	 * @throws ContentException
	 *             Exception in the strat
	 */
	void start(String contentPath);

	/**
	 * TODO
	 * 
	 * @param repositoryItem
	 */
	void start(RepositoryItem repositoryItem);

	/**
	 * TODO
	 * 
	 */
	void start(HttpEndpoint endpoint);

}
