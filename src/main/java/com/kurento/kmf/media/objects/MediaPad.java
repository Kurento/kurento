package com.kurento.kmf.media.objects;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kms.thrift.api.MediaServerException;

public abstract class MediaPad extends MediaObject {

	public MediaPad(MediaPadRefDTO objectRef) {
		super(objectRef);
	}

	/**
	 * Obtains the {@link MediaElement} that encloses this pad
	 * 
	 * @return
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public MediaElement getMediaElement() throws KurentoMediaFrameworkException {
		return (MediaElement) getParent();
	}

	/**
	 * Obtains the {@link MediaElement} that encloses this pad
	 * 
	 * @return
	 * @throws MediaServerException
	 * @throws InvokationException
	 */
	public void getMediaElement(final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {
		super.getParent(cont);
	}

}
