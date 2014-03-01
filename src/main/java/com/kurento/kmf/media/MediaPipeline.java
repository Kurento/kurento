package com.kurento.kmf.media;

import com.kurento.tool.rom.server.FactoryMethod;
import com.kurento.tool.rom.server.Param;

public interface MediaPipeline extends MediaObject {

	@FactoryMethod
	public abstract PlayerEndpoint.Builder newPlayerEndpoint(
			@Param("uri") String uri);

	@FactoryMethod
	public abstract HttpGetEndpoint.Builder newHttpGetEndpoint();

	@FactoryMethod
	public abstract WebRtcEndpoint.Builder newWebRtcEndpoint();

	@FactoryMethod
	public abstract ZBarFilter.Builder newZBarFilter();

	@FactoryMethod
	public abstract PointerDetectorAdvFilter.Builder newPointerDetectorAdvFilter(
			@Param("calibrationRegion") WindowParam calibrationRegion);

	@FactoryMethod
	public abstract HttpPostEndpoint.Builder newHttpPostEndpoint();

	@FactoryMethod
	public abstract RtpEndpoint.Builder newRtpEndpoint();

	@FactoryMethod
	public abstract PointerDetectorFilter.Builder newPointerDetectorFilter();

	@FactoryMethod
	public abstract ChromaFilter.Builder newChromaFilter(
			@Param("window") WindowParam window);

	@FactoryMethod
	public abstract JackVaderFilter.Builder newJackVaderFilter();

	@FactoryMethod
	public abstract FaceOverlayFilter.Builder newFaceOverlayFilter();

	@FactoryMethod
	public abstract PlateDetectorFilter.Builder newPlateDetectorFilter();

	@FactoryMethod
	public abstract RecorderEndpoint.Builder newRecorderEndpoint(
			@Param("uri") String uri);

	@FactoryMethod
	public abstract GStreamerFilter.Builder newGStreamerFilter(
			@Param("command") String command);

	public interface Factory {

		public Builder create();
	}

	public interface Builder extends AbstractBuilder<MediaPipeline> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
