package com.kurento.kmf.media;

import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.server.Param;

public interface ZBarFilter extends Filter {

	ListenerRegistration addCodeFoundListener(
			MediaEventListener<CodeFoundEvent> listener);

	void addCodeFoundListener(MediaEventListener<CodeFoundEvent> listener,
			Continuation<ListenerRegistration> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<ZBarFilter> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
