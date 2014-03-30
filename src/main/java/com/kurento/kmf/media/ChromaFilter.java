/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

/**
 * 
 * ChromaFilter interface. This type of {@link Filter} makes transparent a
 * colour range in the top layer, revealing another image behind
 * 
 **/
@RemoteClass
public interface ChromaFilter extends Filter {

	/**
	 * 
	 * Sets the image to show on the detected chroma surface.
	 * 
	 * @param uri
	 *            URI where the image is located
	 * 
	 **/
	void setBackground(@Param("uri") String uri);

	/**
	 * 
	 * Asynchronous version of setBackground: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see ChromaFilter#setBackground
	 * 
	 * @param uri
	 *            URI where the image is located
	 * 
	 **/
	void setBackground(@Param("uri") String uri, Continuation<Void> cont);

	/**
	 * 
	 * Clears the image used to be shown behind the chroma surface.
	 * 
	 **/
	void unsetBackground();

	/**
	 * 
	 * Asynchronous version of unsetBackground: {@link Continuation#onSuccess}
	 * is called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see ChromaFilter#unsetBackground
	 * 
	 **/
	void unsetBackground(Continuation<Void> cont);

	/**
	 * 
	 * Factory for building {@link ChromaFilter}
	 * 
	 **/
	public interface Factory {
		/**
		 * 
		 * Creates a Builder for ChromaFilter
		 * 
		 * @param mediaPipeline
		 * 
		 **/
		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("window") WindowParam window);
	}

	public interface Builder extends AbstractBuilder<ChromaFilter> {

		/**
		 * 
		 * Sets a value for mediaPipeline in Builder for ChromaFilter.
		 * 
		 * @param mediaPipeline
		 *            the {@link MediaPipeline} to which the filter belongs
		 * 
		 **/
		public Builder withMediaPipeline(MediaPipeline mediaPipeline);

		/**
		 * 
		 * Sets a value for window in Builder for ChromaFilter.
		 * 
		 * @param window
		 *            Window of replacement for the {@link ChromaFilter}
		 * 
		 **/
		public Builder withWindow(WindowParam window);

		/**
		 * 
		 * Sets a value for backgroundImage in Builder for ChromaFilter.
		 * 
		 * @param backgroundImage
		 *            url of image to be used to replace the detected background
		 * 
		 **/
		public Builder withBackgroundImage(String backgroundImage);
	}
}
