package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface ChromaFilter extends Filter {

    void setBackground(@Param("uri") String uri);
    void setBackground(@Param("uri") String uri, Continuation<Void> cont);

    void unsetBackground();
    void unsetBackground(Continuation<Void> cont);


	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("window") WindowParam window);
    }

    public interface Builder extends AbstractBuilder<ChromaFilter> {

        public Builder withBackgroundImage(String backgroundImage);
        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
