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
