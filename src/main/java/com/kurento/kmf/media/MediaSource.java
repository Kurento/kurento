package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface MediaSource extends MediaPad {

	List<MediaSink> getConnectedSinks();

	void getConnectedSinks(Continuation<List<MediaSink>> cont);

	void connect(@Param("sink") MediaSink sink);

	void connect(@Param("sink") MediaSink sink, Continuation<Void> cont);

}
