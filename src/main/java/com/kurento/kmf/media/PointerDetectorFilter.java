/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import java.util.List;

import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.events.WindowOutEvent;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * This type of {@link Filter} detects pointers in a video feed.
 * 
 **/
@RemoteClass
public interface PointerDetectorFilter extends Filter {

	/**
	 * 
	 * Adds a pointer detector window. When a pointer enters or exits this
	 * window, the filter will raise an event indicating so.
	 * 
	 * @param window
	 *            the detection window
	 * 
	 **/
	void addWindow(@Param("window") PointerDetectorWindowMediaParam window);

	/**
	 * 
	 * Asynchronous version of addWindow: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see PointerDetectorFilter#addWindow
	 * 
	 * @param window
	 *            the detection window
	 * 
	 **/
	void addWindow(@Param("window") PointerDetectorWindowMediaParam window,
			Continuation<Void> cont);

	/**
	 * 
	 * Removes all pointer detector windows
	 * 
	 **/
	void clearWindows();

	/**
	 * 
	 * Asynchronous version of clearWindows: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see PointerDetectorFilter#clearWindows
	 * 
	 **/
	void clearWindows(Continuation<Void> cont);

	/**
	 * 
	 * Removes a pointer detector window
	 * 
	 * @param windowId
	 *            id of the window to be removed
	 * 
	 **/
	void removeWindow(@Param("windowId") String windowId);

	/**
	 * 
	 * Asynchronous version of removeWindow: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see PointerDetectorFilter#removeWindow
	 * 
	 * @param windowId
	 *            id of the window to be removed
	 * 
	 **/
	void removeWindow(@Param("windowId") String windowId,
			Continuation<Void> cont);

	/**
	 * Add a {@link MediaEventListener} for event {@link WindowInEvent}.
	 * Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on WindowInEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addWindowInListener(
			MediaEventListener<WindowInEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event {@link WindowInEvent}.
	 * Asynchronous call. Calls Continuation&lt;ListenerRegistration&gt; when it
	 * has been added.
	 * 
	 * @param listener
	 *            Listener to be called on WindowInEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addWindowInListener(MediaEventListener<WindowInEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * Add a {@link MediaEventListener} for event {@link WindowOutEvent}.
	 * Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on WindowOutEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addWindowOutListener(
			MediaEventListener<WindowOutEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event {@link WindowOutEvent}.
	 * Asynchronous call. Calls Continuation&lt;ListenerRegistration&gt; when it
	 * has been added.
	 * 
	 * @param listener
	 *            Listener to be called on WindowOutEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addWindowOutListener(MediaEventListener<WindowOutEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Factory for building {@link PointerDetectorFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for PointerDetectorFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<PointerDetectorFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for PointerDetectorFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for windows in Builder for PointerDetectorFilter.
		 * 
		 * @param windows
		 *            list of detection windows for the filter to detect
		 *            pointers entering or exiting the window
		 * 
		 **/
		public Builder withWindows(List<PointerDetectorWindowMediaParam> windows);
	}
}
