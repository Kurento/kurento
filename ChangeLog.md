6.2.0
=====

  * RecorderEndpoint: Fix problems with negative timestamps that produced empty
    videos
  * RtpEndpoint now inherits from BaseRtpEndpoint
  * WebRtcEndpoint uses BaseRtpEndpoint configuration for port ranges
  * RtpEndpoint uses BaseRtpEndpoint configuration for port ranges
  * RecorderEndpoint: Fix negotiation problems with MP4 files. Now format
    changes are not allowed
  * RtpEndpoint: add SDES encryption support
  * RecorderEndpoint: internal redesign simplifying internal pipeline
  * PlayerEndpoint: set correct timestamps when source does not provide them
    properly.
  * WebRtcEndpoint: report possible error on candidate handling
  * Composite: fix bugs simplifying internal design
