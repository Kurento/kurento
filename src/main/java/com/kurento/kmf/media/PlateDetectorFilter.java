package com.kurento.kmf.media;

import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.tool.rom.server.Param;

public interface PlateDetectorFilter extends Filter {

	ListenerRegistration addPlateDetectedListener(
			MediaEventListener<PlateDetectedEvent> listener);

	void addPlateDetectedListener(
			MediaEventListener<PlateDetectedEvent> listener,
			Continuation<ListenerRegistration> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<PlateDetectorFilter> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
