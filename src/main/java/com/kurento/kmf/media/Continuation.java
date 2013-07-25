package com.kurento.kmf.media;

/**
 * Used as a callback for some asynchronous actions
 * 
 */
public interface Continuation<T> {

	/**
	 * This method is called when the operation success
	 * 
	 * @param result
	 */
	public void onSuccess(T result);

	/**
	 * This method gets called when the operation fails
	 * 
	 * @param cause
	 *            The cause of the failure
	 */
	public void onError(Throwable cause);

}