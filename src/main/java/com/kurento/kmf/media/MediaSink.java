package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface MediaSink extends MediaPad {

	void disconnect(@Param("src") MediaSource src);

	void disconnect(@Param("src") MediaSource src, Continuation<Void> cont);

	MediaSource getConnectedSrc();

	void getConnectedSrc(Continuation<MediaSource> cont);

}
