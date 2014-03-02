package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;

@RemoteClass
public interface HttpEndpoint extends SessionEndpoint {

	String getUrl();

	void getUrl(Continuation<String> cont);

}
