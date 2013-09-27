package com.kurento.kmf.media;

import com.kurento.kmf.media.internal.MediaElementImpl;
import com.kurento.kms.thrift.api.MediaType;

public interface MediaPad extends MediaObject {

	/**
	 * Obtains the {@link MediaElementImpl} that encloses this pad
	 * 
	 * @return
	 */
	MediaElementImpl getMediaElement();

	/**
	 * Obtains the {@link MediaElementImpl} that encloses this pad
	 * 
	 * @return
	 */
	void getMediaElement(final Continuation<MediaElementImpl> cont);

	MediaType getMediaType();

	String getMediaDescription();
}
