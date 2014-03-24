package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface GStreamerFilter extends Filter {

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("command") String command);
	}

	public interface Builder extends AbstractBuilder<GStreamerFilter> {

	}
}
