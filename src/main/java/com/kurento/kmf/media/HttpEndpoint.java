package com.kurento.kmf.media;


public interface HttpEndpoint extends SessionEndpoint {

	String getUrl();

	void getUrl(Continuation<String> cont);

}
