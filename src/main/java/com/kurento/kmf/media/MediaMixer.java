package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.FactoryMethod;

@RemoteClass
public interface MediaMixer extends MediaObject {

	@FactoryMethod("mediaMixer")
	public abstract MixerPort.Builder newMixerPort();

}
