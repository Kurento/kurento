package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public interface RtpEndpoint extends SdpEndpoint {

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<RtpEndpoint> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
