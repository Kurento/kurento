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
 * This type of {@link Filter} detects UI pointers in a video feed.
 * 
 **/
@RemoteClass
public interface PointerDetectorAdvFilter extends Filter {

	/**
	 * 
	 * Adds a new detection window for the filter to detect pointers entering or
	 * exiting the window
	 * 
	 * @param window
	 *            The window to be added
	 * 
	 **/
	void addWindow(@Param("window") PointerDetectorWindowMediaParam window);

	/**
	 * 
	 * Asynchronous version of addWindow: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see PointerDetectorAdvFilter#addWindow
	 * 
	 * @param window
	 *            The window to be added
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
	 * @see PointerDetectorAdvFilter#clearWindows
	 * 
	 **/
	void clearWindows(Continuation<Void> cont);

	/**
	 * 
	 * This method allows to calibrate the tracking color. The new tracking
	 * color will be the color of the object in the colorCalibrationRegion.
	 * 
	 **/
	void trackColorFromCalibrationRegion();

	/**
	 * 
	 * Asynchronous version of trackColorFromCalibrationRegion:
	 * {@link Continuation#onSuccess} is called when the action is done. If an
	 * error occurs, {@link Continuation#onError} is called.
	 * 
	 * @see PointerDetectorAdvFilter#trackColorFromCalibrationRegion
	 * 
	 **/
	void trackColorFromCalibrationRegion(Continuation<Void> cont);

	/**
	 * 
	 * Removes a window from the list to be monitored
	 * 
	 * @param windowId
	 *            the id of the window to be removed
	 * 
	 **/
	void removeWindow(@Param("windowId") String windowId);

	/**
	 * 
	 * Asynchronous version of removeWindow: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see PointerDetectorAdvFilter#removeWindow
	 * 
	 * @param windowId
	 *            the id of the window to be removed
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
	 * Factory for building {@link PointerDetectorAdvFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for PointerDetectorAdvFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("calibrationRegion") WindowParam calibrationRegion);
	}

	public interface Builder extends AbstractBuilder<PointerDetectorAdvFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for
		 * PointerDetectorAdvFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for calibrationRegion in Builder for
		 * PointerDetectorAdvFilter.
		 * 
		 * @param calibrationRegion
		 *            region to calibrate the filter
		 * 
		 **/
		public Builder withCalibrationRegion(WindowParam calibrationRegion);

		/**
		 * 
		 * Sets a value for windows in Builder for PointerDetectorAdvFilter.
		 * 
		 * @param windows
		 *            list of detection windows for the filter.
		 * 
		 **/
		public Builder withWindows(List<PointerDetectorWindowMediaParam> windows);
	}
}
