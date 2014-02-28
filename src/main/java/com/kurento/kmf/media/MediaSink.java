package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface MediaSink extends MediaPad {

    void disconnect(@Param("src") MediaSource src);
    void disconnect(@Param("src") MediaSource src, Continuation<Void> cont);

    MediaSource getConnectedSrc();
    void getConnectedSrc(Continuation<MediaSource> cont);


	
	
}
