/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media;

import org.kurento.tool.rom.RemoteClass;

/**
 * 
 * Endpoint that enables Kurento to work as an HTTP server, allowing peer HTTP
 * clients to access media.
 * 
 **/
@RemoteClass
public interface HttpEndpoint extends SessionEndpoint {

	/**
	 * 
	 * Obtains the URL associated to this endpoint
	 * 
	 * @return The url as a String *
	 **/
	String getUrl();

	/**
	 * 
	 * Asynchronous version of getUrl: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see HttpEndpoint#getUrl
	 * 
	 **/
	void getUrl(Continuation<String> cont);

}
