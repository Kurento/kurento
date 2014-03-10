package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface DispatcherOneToMany extends Hub {

	void setSource(@Param("source") HubPort source);

	void setSource(@Param("source") HubPort source, Continuation<Void> cont);

	void removeSource();

	void removeSource(Continuation<Void> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<DispatcherOneToMany> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
