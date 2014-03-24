package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface Dispatcher extends Hub {

	void connect(@Param("source") HubPort source, @Param("sink") HubPort sink);

	void connect(@Param("source") HubPort source, @Param("sink") HubPort sink,
			Continuation<Void> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<Dispatcher> {

	}
}
