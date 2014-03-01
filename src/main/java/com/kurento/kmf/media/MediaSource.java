package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.server.Param;

public interface MediaSource extends MediaPad {

	List<MediaSink> getConnectedSinks();

	void getConnectedSinks(Continuation<List<MediaSink>> cont);

	void connect(@Param("sink") MediaSink sink);

	void connect(@Param("sink") MediaSink sink, Continuation<Void> cont);

}
