# MoQ endpoint integration test

`test_moq_endpoints` sends synthetic audio and video through a
`MoqPublisherEndpoint`, a MoQ relay, and a `MoqSubscriberEndpoint`. It passes
only after Kurento reports both media types flowing out of the subscriber.
This exercises the endpoint bridges, encoding/decoding, relay connection, and
media flow; it does not replace a browser/WebRTC tutorial test.

The test is opt-in because it requires the `moq-gst` GStreamer plugin, the
endpoint's GStreamer encoders/decoders, and a reachable relay. Configure and
build the module with the existing experimental-test option enabled:

```bash
cmake -S server/module-elements -B build/module-elements \
  -DENABLE_EXPERIMENTAL_TESTS=ON
cmake --build build/module-elements --target test_moq_endpoints
```

Start an anonymous MoQ relay separately (for example, using the local relay
configuration documented by [moq-dev/moq](https://github.com/moq-dev/moq/tree/main/demo/relay)).
Set `KURENTO_MOQ_TEST_RELAY_URL` to the relay URL accepted by `moq-gst`, then run:

```bash
KURENTO_MOQ_TEST_RELAY_URL=https://localhost:4443/anon \
  ctest --test-dir build/module-elements --output-on-failure \
  -R '^test_moq_endpoints$'
```

The test creates a unique broadcast name for each run. A timeout or a relay,
plugin, or codec error fails the test.
