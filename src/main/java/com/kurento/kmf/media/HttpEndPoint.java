package com.kurento.kmf.media;

public interface HttpEndPoint extends EndPoint {
	/* SYNC */
	String getUrl();

	/* ASYNC */
	void getUrl(final Continuation<String> cont);
}
