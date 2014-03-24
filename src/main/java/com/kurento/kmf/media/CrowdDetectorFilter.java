package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface CrowdDetectorFilter extends Filter {

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("rois") List<RegionOfInterest> rois);
	}

	public interface Builder extends AbstractBuilder<CrowdDetectorFilter> {

	}
}
