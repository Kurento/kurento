package com.kurento.kmf.media;

import java.io.Serializable;


// Implementaciones de este objeto deben incluir:
// Recorder: graba el media a un archivo
// RTPRelay: reemite (multicast/unicast) el media a un flujo RTP con un media type dado
// RTSPPublisher: Reemite (broadcast a través de wowza) el media mediante RTSP
// RTMPPublisher: Reemite el media mediante RTMP (broadcast a través de wowza/Red5, etc.)
// HLSPublisher: Reemite el media mediante HTTP Live Streaming (broadcast a través de ....)
// No hacen falta en el API, porque las construye una factoria a partir de una uri
public interface MediaPlayer extends Serializable {


}
