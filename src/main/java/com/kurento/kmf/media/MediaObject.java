package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface MediaObject  {

    MediaPipeline getMediaPipeline();
    void getMediaPipeline(Continuation<MediaPipeline> cont);

    MediaObject getParent();
    void getParent(Continuation<MediaObject> cont);

    ListenerRegistration addErrorListener(MediaEventListener<ErrorEvent> listener);
    void addErrorListener(MediaEventListener<ErrorEvent> listener, Continuation<ListenerRegistration> cont);

	void release();
	void release(Continuation<Void> continuation);
}
