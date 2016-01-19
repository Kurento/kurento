6.3.0
=====

  * RecorderEndpoint: Fix many problems that appeared with the las gstreamer update
  * RtpEndpoint: Add event to notify when a srtp key is about to expire
  * PlayerEndpoint: Add seek capability
  * WebRtcEndpoint: Fix minor problems with datachannels
  * WebRtcEndpoint: Fix problem with chrome 48 candidates
  * RtpEndpoint: Add support for ipv6, by now only one protocol can be used
  * WebRtcEndpoint: Add tests for ipv6 support
  * WebRtcEndpoint: Do not use TURN configuration until bug in libnice is fixed
    TURN in clients (browsers) can still be used, but kms will not generate
    relay candidates.

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
