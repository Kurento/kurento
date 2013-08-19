package com.kurento.kmf.media;

import java.io.IOException;

import com.kurento.kms.api.MediaObjectId;

public class MixerEndPoint extends EndPoint {

	private static final long serialVersionUID = 1L;

	MixerEndPoint(MediaObjectId mixerEndPointId) {
		super(mixerEndPointId);
	}

	/* SYNC */

	public Mixer getMixer() throws IOException {
		MediaObject parent = getParent();
		if (parent instanceof Mixer) {
			return (Mixer) parent;
		}
		return null;
	}

	/* ASYNC */

	public void getMixer(final Continuation<Mixer> cont) throws IOException {
		getParent(new Continuation<MediaObject>() {
			@Override
			public void onSuccess(MediaObject result) {
				if (result instanceof Mixer) {
					cont.onSuccess((Mixer) result);
				} else {
					cont.onSuccess(null);
				}
			}

			@Override
			public void onError(Throwable cause) {
				cont.onError(cause);
			}
		});
	}

}
