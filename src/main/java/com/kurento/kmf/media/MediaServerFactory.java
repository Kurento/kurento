package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.server.Param;

public abstract class MediaServerFactory {

	public abstract PlayerEndpoint.Builder createPlayerEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("uri") String uri);

	public abstract HttpGetEndpoint.Builder createHttpGetEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract WebRtcEndpoint.Builder createWebRtcEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract ZBarFilter.Builder createZBarFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract HubPort.Builder createHubPort(@Param("hub") Hub hub);

	public abstract PointerDetectorAdvFilter.Builder createPointerDetectorAdvFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("calibrationRegion") WindowParam calibrationRegion);

	public abstract HttpPostEndpoint.Builder createHttpPostEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract RtpEndpoint.Builder createRtpEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract PointerDetectorFilter.Builder createPointerDetectorFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract ChromaFilter.Builder createChromaFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("window") WindowParam window);

	public abstract MediaPipeline.Builder createMediaPipeline();

	public abstract Dispatcher.Builder createDispatcher(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract DispatcherOneToMany.Builder createDispatcherOneToMany(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract Composite.Builder createComposite(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract JackVaderFilter.Builder createJackVaderFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract FaceOverlayFilter.Builder createFaceOverlayFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract PlateDetectorFilter.Builder createPlateDetectorFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract RecorderEndpoint.Builder createRecorderEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("uri") String uri);

	public abstract GStreamerFilter.Builder createGStreamerFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("command") String command);

	public abstract CrowdDetectorFilter.Builder createCrowdDetectorFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("rois") List<RegionOfInterest> rois);
}
