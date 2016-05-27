6.5.0
=====

  * Change license to Apache 2.0
  * WebRctEndpoint: Add information about ice candidates pair selected
  * WebRctEndpoint: Fix memory leaks on candidatesm management
  * WebRctEndpoint: Fix fingerprint generation when certificate is buldled with the key
  * WebRctEndpoint: Fix bugs when using a custom pem por dtls
  * WebRctEndpoint: Improve events names deprecating old ones
    (old ones are still valid, but its use is discouraged)
  * RecorderEndpoint: Fix state management
  * RecorderEndpoint: Add StopAndWait method
  * Improve documentation

6.4.0
=====

  * WebRtcEndpoint: Update libnice library to 0.1.13.1
  * WebRtcEndpoint: Turn is working again now that libnice is updated
  * RecorderEndpoint: Calculate end to end latency stats
  * PlayerEndpoint: Calculate end to end latency
  * WebRtcEndpoint: minor issues fixing
  * RecorderEndpoint: Fix problem when recording to http, now mp4 is buffered
    using and fast start and webm is recorder as live (no seekable without
    post-processing)

6.3.1
=====

  * PlayerEndpoint: Fix problem in pause introduced in previous release
  * WebRtcEndpoint: Fix problems with upper/lower case codec names
  * WebRtcEndpoint: Parse candidates present in original offer correctly
  * RecorderEndpoint: Reduce log level for some messages that were not errors

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
