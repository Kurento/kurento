package com.kurento.kmf.media;

public interface MediaFactory {

	// Construye y devuelve un player a partir de una uri de publicación (file, rtp, rtsp, rtmp, hls, etc.)
	public MediaSink getSink (String uri);
	
	// Construye y devuelve una fuente de medios a partir de una uri de reproducción (file, rtp, rtsp, rtmp, hls, etc.)
	public MediaSrc getSrc (String uri);
}
