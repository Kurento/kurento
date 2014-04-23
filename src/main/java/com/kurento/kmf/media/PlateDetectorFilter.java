/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * PlateDetectorFilter interface. This type of {@link Endpoint} detects vehicle
 * plates in a video feed.
 * 
 **/
@RemoteClass
public interface PlateDetectorFilter extends Filter {

	/**
	 * 
	 * Configures the average width of the license plates in the image
	 * represented as an image percentage.
	 * 
	 * @param plateWidthPercentage
	 *            average width of the license plates represented as an image
	 *            percentage [0..1].
	 * 
	 **/
	void setPlateWidthPercentage(
			@Param("plateWidthPercentage") float plateWidthPercentage);

	/**
	 * 
	 * Asynchronous version of setPlateWidthPercentage:
	 * {@link Continuation#onSuccess} is called when the action is done. If an
	 * error occurs, {@link Continuation#onError} is called.
	 * 
	 * @see PlateDetectorFilter#setPlateWidthPercentage
	 * 
	 * @param plateWidthPercentage
	 *            average width of the license plates represented as an image
	 *            percentage [0..1].
	 * 
	 **/
	void setPlateWidthPercentage(
			@Param("plateWidthPercentage") float plateWidthPercentage,
			Continuation<Void> cont);

	/**
	 * Add a {@link MediaEventListener} for event {@link PlateDetectedEvent}.
	 * Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on PlateDetectedEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addPlateDetectedListener(
			MediaEventListener<PlateDetectedEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event {@link PlateDetectedEvent}.
	 * Asynchronous call. Calls Continuation&lt;ListenerRegistration&gt; when it
	 * has been added.
	 * 
	 * @param listener
	 *            Listener to be called on PlateDetectedEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addPlateDetectedListener(
			MediaEventListener<PlateDetectedEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Factory for building {@link PlateDetectorFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for PlateDetectorFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<PlateDetectorFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for PlateDetectorFilter.
		 * 
		 * @param mediaPipeline
		 *            the parent {@link MediaPipeline} of this
		 *            {@link PlateDetectorFilter}
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
