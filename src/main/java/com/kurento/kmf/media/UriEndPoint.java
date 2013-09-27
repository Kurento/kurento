package com.kurento.kmf.media;

public interface UriEndPoint extends EndPoint {

	String getUri();

	void pause();

	void stop();

	/* ASYNC */
	void getUri(Continuation<String> cont);

	public void pause(final Continuation<Void> cont);

	public void stop(final Continuation<Void> cont);
}
