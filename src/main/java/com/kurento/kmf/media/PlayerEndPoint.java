package com.kurento.kmf.media;

public interface PlayerEndPoint extends UriEndPoint {
	/* SYNC */
	public void play();

	/* ASYNC */
	public void play(final Continuation<Void> cont);
}
