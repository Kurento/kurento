package com.kurento.kmf.media;

public interface Continuation<F> {

	/**
	 * This method is called when the operation succeeds
	 * 
	 * @param result
	 */
	public void onSuccess(F result);

	/**
	 * This method gets called when the operation fails
	 * 
	 * @param cause
	 *            The cause of the failure
	 */
	public void onError(Throwable cause);

}
