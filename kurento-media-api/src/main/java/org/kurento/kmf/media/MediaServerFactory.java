package org.kurento.kmf.media;

import org.kurento.tool.rom.server.Param;

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

	public abstract HttpPostEndpoint.Builder createHttpPostEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract RtpEndpoint.Builder createRtpEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract MediaPipeline.Builder createMediaPipeline();

	public abstract Dispatcher.Builder createDispatcher(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract DispatcherOneToMany.Builder createDispatcherOneToMany(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract Composite.Builder createComposite(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract FaceOverlayFilter.Builder createFaceOverlayFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline);

	public abstract RecorderEndpoint.Builder createRecorderEndpoint(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("uri") String uri);

	public abstract GStreamerFilter.Builder createGStreamerFilter(
			@Param("mediaPipeline") MediaPipeline mediaPipeline,
			@Param("command") String command);

}
