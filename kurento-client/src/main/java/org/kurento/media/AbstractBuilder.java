/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes shoult go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media;

/**
 * Kurento Media Builder base interface
 * 
 * Builds a {@code <T>} object, either synchronously using {@link #build} or
 * asynchronously using {@link #buildAsync}
 * 
 * @param T
 *            the type of object to build
 * 
 **/
public interface AbstractBuilder<T> {

	/**
	 * Builds an object synchronously using the builder design pattern
	 * 
	 * @return <T> The type of object
	 * 
	 **/
	public T build();

	/**
	 * Builds an object asynchronously using the builder design pattern.
	 * 
	 * The continuation will have {@link Continuation#onSuccess} called when the
	 * object is ready, or {@link Continuation#onError} if an error occurs
	 * 
	 * @param continuation
	 *            will be called when the object is built
	 * 
	 * 
	 **/
	public void buildAsync(Continuation<T> continuation);

}
