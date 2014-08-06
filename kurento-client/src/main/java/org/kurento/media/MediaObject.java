/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media;

import org.kurento.media.events.ErrorEvent;
import org.kurento.media.events.MediaEventListener;
import org.kurento.tool.rom.RemoteClass;

/**
 * 
 * Base for all objects that can be created in the media server.
 * 
 **/
@RemoteClass
public interface MediaObject {

	/**
	 * 
	 * Returns the pipeline to which this MediaObject belong, or the pipeline
	 * itself if invoked over a {@link MediaPipeline}
	 * 
	 * @return the MediaPipeline this MediaObject belongs to.
	 * 
	 *         If called on a {@link MediaPipeline} it will return
	 *         <code>this</code>. *
	 **/
	MediaPipeline getMediaPipeline();

	/**
	 * 
	 * Asynchronous version of getMediaPipeline: {@link Continuation#onSuccess}
	 * is called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaObject#getMediaPipeline
	 * 
	 **/
	void getMediaPipeline(Continuation<MediaPipeline> cont);

	/**
	 * 
	 * Returns the parent of this media object. The type of the parent depends
	 * on the type of the element that this method is called upon: The parent of
	 * a {@link MediaPad} is its {@link MediaElement}; the parent of a
	 * {@link MediaMixer} or a {@link MediaElement} is its {@link MediaPipeline}
	 * . A {@link MediaPipeline} has no parent, i.e. the method returns null
	 * 
	 * @return the parent of this MediaObject or null if called on a
	 *         MediaPipeline *
	 **/
	MediaObject getParent();

	/**
	 * 
	 * Asynchronous version of getParent: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaObject#getParent
	 * 
	 **/
	void getParent(Continuation<MediaObject> cont);

	/**
	 * Add a {@link MediaEventListener} for event {@link ErrorEvent}.
	 * Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on ErrorEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addErrorListener(
			MediaEventListener<ErrorEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event {@link ErrorEvent}.
	 * Asynchronous call. Calls Continuation&lt;ListenerRegistration&gt; when it
	 * has been added.
	 * 
	 * @param listener
	 *            Listener to be called on ErrorEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addErrorListener(MediaEventListener<ErrorEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Explicitly release a media object form memory. All of its children will
	 * also be released.
	 * 
	 **/
	void release();

	/**
	 * 
	 * Explicitly release a media object form memory. All of its children will
	 * also be released. Asynchronous call.
	 * 
	 * @param continuation
	 *            {@link #onSuccess(void)} will be called when the actions
	 *            complete. {@link #onError} will be called if there is an
	 *            exception.
	 * 
	 **/
	void release(Continuation<Void> continuation);

}
