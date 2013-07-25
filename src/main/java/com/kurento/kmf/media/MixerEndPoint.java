package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;

public class MixerEndPoint extends EndPoint {

	private static final long serialVersionUID = 1L;

	MixerEndPoint(com.kurento.kms.api.MediaObject mixerPort) {
		super(mixerPort);
	}

	/* SYNC */

	public Mixer getMixer() throws IOException {
		// TODO: Implement using getParent method
		throw new NotImplementedException();
	}

	/* ASYNC */

	public void getMixer(final Continuation<Mixer> cont) throws IOException {
		// TODO: Implement using getParent method
		throw new NotImplementedException();
	}
}
