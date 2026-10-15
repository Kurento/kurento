# MoQ endpoint integration test

`test_moq_endpoints` sends synthetic audio and video through a
`MoqPublisherEndpoint`, a MoQ relay, and a `MoqSubscriberEndpoint`. It passes
only after Kurento reports both media types flowing out of the subscriber.
This exercises the endpoint bridges, encoding/decoding, relay connection, and
media flow; it does not replace a browser/WebRTC tutorial test.

The test is opt-in because it requires the `moq-gst` GStreamer plugin and the
endpoint's GStreamer encoders/decoders. Configure and build the module with
the existing experimental-test option enabled:

```bash
cmake -S server/module-elements -B build/module-elements \
  -DENABLE_EXPERIMENTAL_TESTS=ON
cmake --build build/module-elements --target test_moq_endpoints
```

By default, the test starts an anonymous `moq-relay` process bound to loopback,
waits for it to accept connections, and stops it when the test finishes. Install
the relay executable and make sure it is available in `PATH` (for example,
with `cargo install moq-relay` or the
[official package](https://github.com/moq-dev/moq/tree/main/rs/moq-relay#install)).
The test generates a temporary configuration and chooses an available local
port.

To use an already-running relay instead, set `KURENTO_MOQ_TEST_RELAY_URL` to
the URL accepted by `moq-gst`; this skips the local relay process:

```bash
KURENTO_MOQ_TEST_RELAY_URL=http://localhost:4443 \
  ctest --test-dir build/module-elements --output-on-failure \
  -R '^test_moq_endpoints$'
```

The test creates a unique broadcast name for each run and verifies that both
audio and video flow through the subscriber. A timeout or a relay, plugin, or
codec error fails the test.
