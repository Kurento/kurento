package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface PointerDetectorFilter extends Filter {

    void addWindow(@Param("window") PointerDetectorWindowMediaParam window);
    void addWindow(@Param("window") PointerDetectorWindowMediaParam window, Continuation<Void> cont);

    void clearWindows();
    void clearWindows(Continuation<Void> cont);

    void removeWindow(@Param("windowId") String windowId);
    void removeWindow(@Param("windowId") String windowId, Continuation<Void> cont);

    ListenerRegistration addWindowInListener(MediaEventListener<WindowInEvent> listener);
    void addWindowInListener(MediaEventListener<WindowInEvent> listener, Continuation<ListenerRegistration> cont);
    ListenerRegistration addWindowOutListener(MediaEventListener<WindowOutEvent> listener);
    void addWindowOutListener(MediaEventListener<WindowOutEvent> listener, Continuation<ListenerRegistration> cont);

	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    }

    public interface Builder extends AbstractBuilder<PointerDetectorFilter> {

        public Builder withWindow(PointerDetectorWindowMediaParam window);
        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
