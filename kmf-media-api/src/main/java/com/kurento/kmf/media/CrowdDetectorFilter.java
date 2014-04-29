/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import java.util.List;

import com.kurento.kmf.media.events.CrowdDetectorDirectionEvent;
import com.kurento.kmf.media.events.CrowdDetectorFluidityEvent;
import com.kurento.kmf.media.events.CrowdDetectorOccupancyEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * Filter that detects people agglomeration in video streams
 * 
 **/
@RemoteClass
public interface CrowdDetectorFilter extends Filter {

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link CrowdDetectorFluidityEvent}. Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on CrowdDetectorFluidityEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addCrowdDetectorFluidityListener(
			MediaEventListener<CrowdDetectorFluidityEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link CrowdDetectorFluidityEvent}. Asynchronous call. Calls
	 * Continuation&lt;ListenerRegistration&gt; when it has been added.
	 * 
	 * @param listener
	 *            Listener to be called on CrowdDetectorFluidityEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addCrowdDetectorFluidityListener(
			MediaEventListener<CrowdDetectorFluidityEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link CrowdDetectorOccupancyEvent}. Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on CrowdDetectorOccupancyEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addCrowdDetectorOccupancyListener(
			MediaEventListener<CrowdDetectorOccupancyEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link CrowdDetectorOccupancyEvent}. Asynchronous call. Calls
	 * Continuation&lt;ListenerRegistration&gt; when it has been added.
	 * 
	 * @param listener
	 *            Listener to be called on CrowdDetectorOccupancyEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addCrowdDetectorOccupancyListener(
			MediaEventListener<CrowdDetectorOccupancyEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link CrowdDetectorDirectionEvent}. Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on CrowdDetectorDirectionEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addCrowdDetectorDirectionListener(
			MediaEventListener<CrowdDetectorDirectionEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link CrowdDetectorDirectionEvent}. Asynchronous call. Calls
	 * Continuation&lt;ListenerRegistration&gt; when it has been added.
	 * 
	 * @param listener
	 *            Listener to be called on CrowdDetectorDirectionEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addCrowdDetectorDirectionListener(
			MediaEventListener<CrowdDetectorDirectionEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Factory for building {@link CrowdDetectorFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for CrowdDetectorFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("rois") List<RegionOfInterest> rois);
	}

	public interface Builder extends AbstractBuilder<CrowdDetectorFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for CrowdDetectorFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for rois in Builder for CrowdDetectorFilter.
		 * 
		 * @param rois
		 *            Regions of interest for the filter
		 * 
		 **/
		public Builder withRois(List<RegionOfInterest> rois);
	}
}
