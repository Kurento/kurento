package com.kurento.kmf.media;

import java.util.List;

import com.kurento.kmf.media.events.CrowdDetectorDirectionEvent;
import com.kurento.kmf.media.events.CrowdDetectorFluidityEvent;
import com.kurento.kmf.media.events.CrowdDetectorOccupancyEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface CrowdDetectorFilter extends Filter {

	ListenerRegistration addCrowdDetectorFluidityListener(
			MediaEventListener<CrowdDetectorFluidityEvent> listener);

	void addCrowdDetectorFluidityListener(
			MediaEventListener<CrowdDetectorFluidityEvent> listener,
			Continuation<ListenerRegistration> cont);

	ListenerRegistration addCrowdDetectorOccupancyListener(
			MediaEventListener<CrowdDetectorOccupancyEvent> listener);

	void addCrowdDetectorOccupancyListener(
			MediaEventListener<CrowdDetectorOccupancyEvent> listener,
			Continuation<ListenerRegistration> cont);

	ListenerRegistration addCrowdDetectorDirectionListener(
			MediaEventListener<CrowdDetectorDirectionEvent> listener);

	void addCrowdDetectorDirectionListener(
			MediaEventListener<CrowdDetectorDirectionEvent> listener,
			Continuation<ListenerRegistration> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline,
				@Param("rois") List<RegionOfInterest> rois);
	}

	public interface Builder extends AbstractBuilder<CrowdDetectorFilter> {

	}
}
