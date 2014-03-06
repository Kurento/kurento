package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.FactoryMethod;

@RemoteClass
public interface Hub extends MediaObject {

	@FactoryMethod("hub")
	public abstract MixerPort.Builder newMixerPort();

}
