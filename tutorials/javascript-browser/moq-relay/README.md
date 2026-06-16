kurento-moq-relay
==================

> :warning: **Warning**
>
> Bower dependencies are not yet upgraded for Kurento 7.0.0.
>
> Kurento tutorials that use pure browser JavaScript need to be rewritten to drop the deprecated Bower service and instead use a web resource packer. This has not been done, so these tutorials won't be able to download the dependencies they need to work. PRs would be appreciated!

Kurento JavaScript Tutorial: WebRTC publisher to MoQ relay and MoQ relay subscriber to WebRTC.

Running this tutorial
---------------------

1. Install the browser tutorial dependencies:

   ```bash
   cd /home/runner/work/kurento/kurento/tutorials/javascript-browser/moq-relay
   bower install
   ```

2. Start a static HTTPS server. For convenience, this example reuses the test certificate that already exists in the `hello-world` tutorial:

   ```bash
   http-server -p 8443 -S \
     -C ../hello-world/keys/server.crt \
     -K ../hello-world/keys/server.key
   ```

3. Open `https://localhost:8443/` in a WebRTC-capable browser.
4. Set the Kurento WebSocket URL, the MoQ relay URL, and the broadcast name.
5. Click **Start publishing** to send the local WebRTC camera/microphone to the MoQ relay.
6. Click **Start viewing** to subscribe to the same MoQ broadcast and receive it back in the browser through WebRTC.

The demo is browser-only: it talks directly to Kurento Media Server with `kurento-client-js`, without any custom application server.
