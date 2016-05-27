[![][KurentoImage]][website]

Copyright © 2013-2016 [Kurento]. Licensed under [Apache 2.0 License].

JavaScript Kurento Client - Tests
=================================
Tests are autonomous, only requirement is to exec previously ```npm install```
to have installed all the dev dependencies.

## Environments

### Browser

To exec tests in browser, you need to build the browser version of the library
with ```node_modules/.bin/grunt```.

After that, just open the file ```test/index.html``` with any browser, and the
tests will launch automatically using [QUnit]. In case of the browser raising
security policy errors, you can host the code using any static web server from
the source code root folder, for example using the command
```python -m SimpleHTTPServer 8000```.

You can be able to configure to what WebSocket endpoint you want to connect on
the dropdown at the top of the tests page.

### Node.js

To exec test in Node.js, you only need to exec ```npm test``` that will launch
all the tests automatically using [QUnit-cli].

If you need to use a WebSocket endpoint different from the default one, you can exec the underlying test command with
```node_modules/.bin/qunit-cli -c kurentoClient:. -c wock:node_modules/wock -c test/_common.js -c test/_proxy.js test/*.js``` and append the *ws_uri* parameter.


# BasicPipeline

## Creation

Basic pipeline reading a video from its URL and stream it over HTTP

### Assertions

* player endpoint is set
* HttpGet endpoint is set
* pipeline URL is set


# FaceOverlayFilter

## Detect face in a video

Play a video setting a face overlay and capture an End of Stream event

### Assertions

* player endpoint is set
* FaceOverlay filter is set
* End of Stream event is received


# GStreamerFilter

## End of Stream

Play a video using an ad-hoc GStreamer filter and capture an End of Stream event

### Assertions

* player endpoint is set
* GStreamer filter is set
* connection of elements was successful
* End of Stream event is received


# PlateDetectorFilter

## Detect plate in a video

Check if a plate is detected on a video

### Assertions

* a plate detected event is dispatched


# PlayerEndpoint

## Play, Pause & Stop

Check player stream operations

### Assertions

* player endpoint is set
* player start to play
* player pause
* player stops

## End of Stream

Player receives end of stream

### Assertions

* player receives end of stream event


# PointerDetectorFilter

## Detect pointer

Detector receives a WindowIn event

### Assertions

* WindowIn event received

## Window events

Detector receives WindowIn and WindowOut events

### Assertions

* WindowIn event received
* WindowOut event received

## Window overlay

Detector is over the image

### Assertions

* WindowIn event received
* WindowOut event received


# ZBarFilter

## Create pipeline and play video

Create a pipeline with a ZBar filter

### Assertions

* player endpoint is set
* zbar filter is set
* player and zbar are connected

## Detect bar-code in a video

Detect a bar code inside the video stream

### Assertions

* bar code is found


[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[website]: http://kurento.org
