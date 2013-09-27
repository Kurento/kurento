package com.kurento.kmf.media;

public interface RecorderEndPoint extends UriEndPoint {
	/* SYNC */
	public void record();

	/* ASYNC */
	public void record(final Continuation<Void> cont);
}
