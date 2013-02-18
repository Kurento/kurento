package com.kurento.kmf.media;

import java.io.Serializable;

// Implementaciones de este interfaz incluyen:
// FilePlayer: Envía un medio encapsulado en un fichero
// RTPSource: Medios en directo de un flujo RTP (cámara ip, etc.)
// RTMPsource: Medios en directo de un flujo RTMP (webcam, etc.)
// RTSPSource: Medios en remoto a través de RTSP
// HLSSrouce: Medios en remoto (y quizá en directo) a través de HTTP Live Streaming
//No hacen falta en el API, porque las construye una factoria a partir de una uri
public interface MediaSource extends Serializable {

	
}
