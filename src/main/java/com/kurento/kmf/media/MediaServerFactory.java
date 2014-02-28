package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public abstract class MediaServerFactory {

    public abstract PlayerEndpoint.Builder createPlayerEndpoint(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("uri") String uri);
    public abstract HttpGetEndpoint.Builder createHttpGetEndpoint(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract WebRtcEndpoint.Builder createWebRtcEndpoint(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract ZBarFilter.Builder createZBarFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract PointerDetectorAdvFilter.Builder createPointerDetectorAdvFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("calibrationRegion") WindowParam calibrationRegion);
    public abstract HttpPostEndpoint.Builder createHttpPostEndpoint(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract RtpEndpoint.Builder createRtpEndpoint(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract PointerDetectorFilter.Builder createPointerDetectorFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract ChromaFilter.Builder createChromaFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("window") WindowParam window);
    public abstract MediaPipeline.Builder createMediaPipeline();
    public abstract JackVaderFilter.Builder createJackVaderFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract FaceOverlayFilter.Builder createFaceOverlayFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract PlateDetectorFilter.Builder createPlateDetectorFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    public abstract RecorderEndpoint.Builder createRecorderEndpoint(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("uri") String uri);
    public abstract GStreamerFilter.Builder createGStreamerFilter(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("command") String command);
}
