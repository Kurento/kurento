package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;

@RemoteClass
public interface UriEndpoint extends Endpoint {

	String getUri();

	void getUri(Continuation<String> cont);

	void pause();

	void pause(Continuation<Void> cont);

	void stop();

	void stop(Continuation<Void> cont);

}
