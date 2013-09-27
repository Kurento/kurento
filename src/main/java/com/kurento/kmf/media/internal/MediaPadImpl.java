package com.kurento.kmf.media.internal;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaPad;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kms.thrift.api.MediaType;

public abstract class MediaPadImpl extends AbstractMediaObject implements
		MediaPad {

	public MediaPadImpl(MediaPadRefDTO objectRef) {
		super(objectRef);
	}

	@Override
	public MediaElementImpl getMediaElement()
			throws KurentoMediaFrameworkException {
		return (MediaElementImpl) getParent();
	}

	@Override
	public void getMediaElement(final Continuation<MediaElementImpl> cont)
			throws KurentoMediaFrameworkException {
		super.getParent(cont);
	}

	@Override
	public MediaType getMediaType() {
		return ((MediaPadRefDTO) objectRef).getType();
	}

	@Override
	public String getMediaDescription() {
		return ((MediaPadRefDTO) objectRef).getMediaDescription();
	}

}
