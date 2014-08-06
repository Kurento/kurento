/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media;

import org.kurento.tool.rom.RemoteClass;

/**
 * 
 * A {@link MediaPad} is an element´s interface with the outside world. The data
 * streams flow from the {@link MediaSource} pad to another element's
 * {@link MediaSink} pad.
 * 
 **/
@RemoteClass
public interface MediaPad extends MediaObject {

	/**
	 * 
	 * Obtains the {@link MediaElement} that encloses this pad
	 * 
	 * @return the element *
	 **/
	MediaElement getMediaElement();

	/**
	 * 
	 * Asynchronous version of getMediaElement: {@link Continuation#onSuccess}
	 * is called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaPad#getMediaElement
	 * 
	 **/
	void getMediaElement(Continuation<MediaElement> cont);

	/**
	 * 
	 * Obtains the type of media that this pad accepts
	 * 
	 * @return One of {@link #MediaType.AUDIO}, {@link #MediaType.DATA} or
	 *         {@link #MediaType.VIDEO} *
	 **/
	MediaType getMediaType();

	/**
	 * 
	 * Asynchronous version of getMediaType: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaPad#getMediaType
	 * 
	 **/
	void getMediaType(Continuation<MediaType> cont);

	/**
	 * 
	 * Obtains the description for this pad. This method does not make a request
	 * to the media server, and is included to keep the simmetry with the rest
	 * of methods from the API.
	 * 
	 * @return The description *
	 **/
	String getMediaDescription();

	/**
	 * 
	 * Asynchronous version of getMediaDescription:
	 * {@link Continuation#onSuccess} is called when the action is done. If an
	 * error occurs, {@link Continuation#onError} is called.
	 * 
	 * @see MediaPad#getMediaDescription
	 * 
	 **/
	void getMediaDescription(Continuation<String> cont);

}
