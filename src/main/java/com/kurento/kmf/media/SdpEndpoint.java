package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface SdpEndpoint extends SessionEndpoint {

    String generateOffer();
    void generateOffer(Continuation<String> cont);

    String processOffer(@Param("offer") String offer);
    void processOffer(@Param("offer") String offer, Continuation<String> cont);

    String processAnswer(@Param("answer") String answer);
    void processAnswer(@Param("answer") String answer, Continuation<String> cont);

    String getLocalSessionDescriptor();
    void getLocalSessionDescriptor(Continuation<String> cont);

    String getRemoteSessionDescriptor();
    void getRemoteSessionDescriptor(Continuation<String> cont);


	
	
}
