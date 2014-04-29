/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * This filter detects <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-qr">QR</a> codes
 * in a video feed. When a code is found, the filter raises a
 * :rom:evnt:`CodeFound` event.
 * 
 **/
@RemoteClass
public interface ZBarFilter extends Filter {

	/**
	 * Add a {@link MediaEventListener} for event {@link CodeFoundEvent}.
	 * Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on CodeFoundEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addCodeFoundListener(
			MediaEventListener<CodeFoundEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event {@link CodeFoundEvent}.
	 * Asynchronous call. Calls Continuation&lt;ListenerRegistration&gt; when it
	 * has been added.
	 * 
	 * @param listener
	 *            Listener to be called on CodeFoundEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addCodeFoundListener(MediaEventListener<CodeFoundEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Factory for building {@link ZBarFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for ZBarFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<ZBarFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for ZBarFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);
	}
}
