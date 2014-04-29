/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;

/**
 * 
 * Interface for endpoints the require a URI to work. An example of this, would
 * be a {@link PlayerEndpoint} whose URI property could be used to locate a file
 * to stream through its {@link MediaSource}
 * 
 **/
@RemoteClass
public interface UriEndpoint extends Endpoint {

	/**
	 * 
	 * Returns the uri for this endpoint.
	 * 
	 * @return the uri as a String *
	 **/
	String getUri();

	/**
	 * 
	 * Asynchronous version of getUri: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see UriEndpoint#getUri
	 * 
	 **/
	void getUri(Continuation<String> cont);

	/**
	 * 
	 * Pauses the feed
	 * 
	 **/
	void pause();

	/**
	 * 
	 * Asynchronous version of pause: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see UriEndpoint#pause
	 * 
	 **/
	void pause(Continuation<Void> cont);

	/**
	 * 
	 * Stops the feed
	 * 
	 **/
	void stop();

	/**
	 * 
	 * Asynchronous version of stop: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see UriEndpoint#stop
	 * 
	 **/
	void stop(Continuation<Void> cont);

}
