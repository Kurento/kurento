package com.kurento.kmf.content.internal;

/**
 * 
 * Declaration of the thread instances that composes the pool handled in Content
 * Management API.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface RejectableRunnable extends Runnable {

	/**
	 * Execution rejected event declaration.
	 */
	void onExecutionRejected();
}
