package com.kurento.kmf.media;

import java.util.List;

import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.events.WindowOutEvent;
import com.kurento.tool.rom.server.Param;

public interface PointerDetectorAdvFilter extends Filter {

	void addWindow(@Param("window") PointerDetectorWindowMediaParam window);

	void addWindow(@Param("window") PointerDetectorWindowMediaParam window,
			Continuation<Void> cont);

	void clearWindows();

	void clearWindows(Continuation<Void> cont);

	void trackcolourFromCalibrationRegion();

	void trackcolourFromCalibrationRegion(Continuation<Void> cont);

	void removeWindow(@Param("windowId") String windowId);

	void removeWindow(@Param("windowId") String windowId,
			Continuation<Void> cont);

	ListenerRegistration addWindowInListener(
			MediaEventListener<WindowInEvent> listener);

	void addWindowInListener(MediaEventListener<WindowInEvent> listener,
			Continuation<ListenerRegistration> cont);

	ListenerRegistration addWindowOutListener(
			MediaEventListener<WindowOutEvent> listener);

	void addWindowOutListener(MediaEventListener<WindowOutEvent> listener,
			Continuation<ListenerRegistration> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("calibrationRegion") WindowParam calibrationRegion);
	}

	public interface Builder extends AbstractBuilder<PointerDetectorAdvFilter> {

		public Builder withWindows(List<PointerDetectorWindowMediaParam> windows);

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
