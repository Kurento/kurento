/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media;

/**
 * This interface is to be used in asynchronous calls to the media server.
 * 
 * @param <F>
 *            The data type of the callback´s response in case of successful
 *            outcome.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface Continuation<F> {

	/**
	 * This method is called when the operation succeeds
	 * 
	 * @param result
	 *            The result of the completed operation
	 */
	void onSuccess(F result) throws Exception;

	/**
	 * This method gets called when the operation fails
	 * 
	 * @param cause
	 *            The cause of the failure
	 */
	void onError(Throwable cause) throws Exception;

}
