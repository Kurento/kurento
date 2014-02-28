package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface FaceOverlayFilter extends Filter {

    void unsetOverlayedImage();
    void unsetOverlayedImage(Continuation<Void> cont);

    void setOverlayedImage(@Param("uri") String uri, @Param("offsetXPercent") float offsetXPercent, @Param("offsetYPercent") float offsetYPercent, @Param("widthPercent") float widthPercent, @Param("heightPercent") float heightPercent);
    void setOverlayedImage(@Param("uri") String uri, @Param("offsetXPercent") float offsetXPercent, @Param("offsetYPercent") float offsetYPercent, @Param("widthPercent") float widthPercent, @Param("heightPercent") float heightPercent, Continuation<Void> cont);


	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    }

    public interface Builder extends AbstractBuilder<FaceOverlayFilter> {

        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
