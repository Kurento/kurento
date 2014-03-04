package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.FactoryMethod;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface MediaPipeline extends MediaObject {

	@FactoryMethod("mediaPipeline")
	public abstract PlayerEndpoint.Builder newPlayerEndpoint(
			@Param("uri") String uri);

	@FactoryMethod("mediaPipeline")
	public abstract HttpGetEndpoint.Builder newHttpGetEndpoint();

	@FactoryMethod("mediaPipeline")
	public abstract WebRtcEndpoint.Builder newWebRtcEndpoint();

	@FactoryMethod("mediaPipeline")
	public abstract ZBarFilter.Builder newZBarFilter();

	@FactoryMethod("mediaPipeline")
	public abstract PointerDetectorAdvFilter.Builder newPointerDetectorAdvFilter(
			@Param("calibrationRegion") WindowParam calibrationRegion);

	@FactoryMethod("mediaPipeline")
	public abstract HttpPostEndpoint.Builder newHttpPostEndpoint();

	@FactoryMethod("mediaPipeline")
	public abstract RtpEndpoint.Builder newRtpEndpoint();

	@FactoryMethod("mediaPipeline")
	public abstract PointerDetectorFilter.Builder newPointerDetectorFilter();

	@FactoryMethod("mediaPipeline")
	public abstract ChromaFilter.Builder newChromaFilter(
			@Param("window") WindowParam window);

	@FactoryMethod("mediaPipeline")
	public abstract DispatcherMixer.Builder newDispatcherMixer();

	@FactoryMethod("mediaPipeline")
	public abstract JackVaderFilter.Builder newJackVaderFilter();

	@FactoryMethod("mediaPipeline")
	public abstract FaceOverlayFilter.Builder newFaceOverlayFilter();

	@FactoryMethod("mediaPipeline")
	public abstract PlateDetectorFilter.Builder newPlateDetectorFilter();

	@FactoryMethod("mediaPipeline")
	public abstract RecorderEndpoint.Builder newRecorderEndpoint(
			@Param("uri") String uri);

	@FactoryMethod("mediaPipeline")
	public abstract GStreamerFilter.Builder newGStreamerFilter(
			@Param("command") String command);

	public interface Factory {

		public Builder create();
	}

	public interface Builder extends AbstractBuilder<MediaPipeline> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
