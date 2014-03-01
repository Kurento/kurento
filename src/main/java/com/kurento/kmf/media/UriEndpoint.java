package com.kurento.kmf.media;


public interface UriEndpoint extends Endpoint {

	String getUri();

	void getUri(Continuation<String> cont);

	void pause();

	void pause(Continuation<Void> cont);

	void stop();

	void stop(Continuation<Void> cont);

}
