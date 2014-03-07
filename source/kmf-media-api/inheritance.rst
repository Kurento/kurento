%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Inheritance hierarchy of main Media objects
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Media Objects
=============

.. digraph:: Media_Objects
   :caption: Media Object Inheritance Hierarchy

   size="12,8";
   fontname = "Bitstream Vera Sans"
   fontsize = 8

   node [
            fontname = "Bitstream Vera Sans"
            fontsize = 8
            shape = "record"
   ]

   edge [
            fontname = "Bitstream Vera Sans"
            fontsize = 8
   ]

   MediaObject [
                label = "{/MediaObject/|\l|" +
                        "+ getMediaPipeline() : MediaPipeline\l" +
                        "+ getParent() : MediaObject[]\l}"
                labelurl = "MediaObject"
                href = "com/kurento/kmf/media/MediaObject.html"
   ]

   MediaElement [
                label = "{/MediaElement/|\l|" +
                        "+ connect(...) : void\l" +
                        "+ getMediaSinks(...) : MediaSink[]\l" +
                        "+ getMediaSrcs(...) : MediaSource[]\l}"
                urllabel = "MediaElement"
                href = "com/kurento/kmf/media/MediaElement.html"
        ]

   edge [
                arrowhead = "empty"
   ]

   MediaObject ->    MediaElement
                     MediaElement -> Endpoint;
                     MediaElement -> Filter;
   MediaObject   ->  MediaMixer;
                     MediaMixer -> MainMixer;
   MediaObject  -> MediaPad;
                   MediaPad -> MediaSink;
                   MediaPad -> MediaSource;
   MediaObject  -> MediaPipeline;

Different Endpoints
===================

.. graph:: Endpoints
   :caption: Inheritance hierarchy of Kurento Endpoints

   size="12,8";
   "Endpoint" -- "SessionEndpoint";
                 "SessionEndpoint" -- "HttpEndpoint";
                                      "HttpEndpoint" -- "HttpGetEndpoint";
                                      "HttpEndpoint" -- "HttpPostEndpoint";
                 "SessionEndpoint" -- "SdpEndpoint";
                                      "SdpEndpoint" -- "RtpEndpoint";
                                      "SdpEndpoint" -- "WebRtcEndpoint";
   "Endpoint" -- "UriEndpoint";
                 "UriEndpoint" -- "PlayerEndpoint";
                 "UriEndpoint" -- "RecorderEndpoint";

Some Filter Classes
===================

.. graph:: Filter_classes
   :caption: Inheritance Hierarchy of Kurento filters

   size="12,8";
   "Filter" -- "ChromaFilter";
    "Filter" -- "ZBarFilter";
    "Filter" -- "PointerDetectorAdvFilter";
    "Filter" -- "PointerDetectorFilter";
    "Filter" -- "JackVaderFilter";
    "Filter" -- "FaceOverlayFilter";
    "Filter" -- "PlateDetectorFilter";
    "Filter" -- "GStreamerFilter";

