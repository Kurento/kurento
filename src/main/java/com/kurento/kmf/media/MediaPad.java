package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface MediaPad extends MediaObject {

    MediaElement getMediaElement();
    void getMediaElement(Continuation<MediaElement> cont);

    MediaType getMediaType();
    void getMediaType(Continuation<MediaType> cont);

    String getMediaDescription();
    void getMediaDescription(Continuation<String> cont);


	
	
}
