%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Inheritance hierarchy of main Media objects
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Media Objects
=============

.. graph:: Media_Objects
   :caption: Media Object Inheritance Hierarchy

   size="12,8";
   "MediaObject"  -- "MediaElement";
                     "MediaElement" -- "EndPoint"[href="http://google.com"];
                                       //"EndPoint" -- "SessionEndpoint";
                                       //              "SessionEndpoint" -- "HttpEndpoint";
                                       //                                   "HttpEndpoint" -- "HttpGetEndpoint";
                                       //                                   "HttpEndpoint" -- "HttpPostEndpoint";
                                       //              "SessionEndpoint" -- "SdpEndpoint";
                                       //                                   "SdpEndpoint" -- "RtpEndpoint";
                                       //                                   "SdpEndpoint" -- "WebRtcEndpoint";
                                       //"EndPoint" -- "UriEndpoint";
                                       //              "UriEndpoint" -- "PlayerEndpoint";
                                       //              "UriEndpoint" -- "RecorderEndpoint";
                     "MediaElement" -- "Filter";
                                   //  "Filter" -- "ChromaFilter";
                                   //  "Filter" -- "ZBarFilter";
                                   //  "Filter" -- "PointerDetectorAdvFilter";
                                   //  "Filter" -- "PointerDetectorFilter";
                                   //  "Filter" -- "JackVaderFilter";
                                   //  "Filter" -- "FaceOverlayFilter";
                                   //  "Filter" -- "PlateDetectorFilter";
                                   //  "Filter" -- "GStreamerFilter";
   "MediaObject"  -- "MediaMixer";
                     "MediaMixer" -- "MainMixer";
   "MediaObject"  -- "MediaPad";
                     "MediaPad" -- "MediaSink";
                     "MediaPad" -- "MediaSource";
   "MediaObject"  -- "MediaPipeline";

Different Endpoints
===================

.. graph:: Endpoints
   :caption: Inheritance hierarchy of Kurento Endpoints

   size="12,8";
   "EndPoint" -- "SessionEndpoint";
                 "SessionEndpoint" -- "HttpEndpoint";
                                      "HttpEndpoint" -- "HttpGetEndpoint";
                                      "HttpEndpoint" -- "HttpPostEndpoint";
                 "SessionEndpoint" -- "SdpEndpoint";
                                      "SdpEndpoint" -- "RtpEndpoint";
                                      "SdpEndpoint" -- "WebRtcEndpoint";
   "EndPoint" -- "UriEndpoint";
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

