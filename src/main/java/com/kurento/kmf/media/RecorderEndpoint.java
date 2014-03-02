package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface RecorderEndpoint extends UriEndpoint {

	void record();

	void record(Continuation<Void> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("uri") String uri);
	}

	public interface Builder extends AbstractBuilder<RecorderEndpoint> {

		public Builder withMediaProfile(MediaProfileSpecType mediaProfile);

		public Builder stopOnEndOfStream();

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
