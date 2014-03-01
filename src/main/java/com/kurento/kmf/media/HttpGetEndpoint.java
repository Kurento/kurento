package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public interface HttpGetEndpoint extends HttpEndpoint {

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<HttpGetEndpoint> {

		public Builder terminateOnEOS();

		public Builder withMediaProfile(MediaProfileSpecType mediaProfile);

		public Builder withDisconnectionTimeout(int disconnectionTimeout);

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
