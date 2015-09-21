# Supported tags

* 6.1.0 ([kurento-media-server/Dockerfile](https://github.com/kurento/kurento-docker/blob/master/kurento-media-server/Dockerfile))

![logo](http://www.kurento.org/sites/default/files/kurento.png)

# Kurento

Kurento is a WebRTC media server and a set of client APIs making simple the development of advanced video applications for WWW and smartphone platforms. Kurento Media Server features include group communications, transcoding, recording, mixing, broadcasting and routing of audiovisual flows.

As a differential feature, Kurento Media Server also provides advanced media processing capabilities involving computer vision, video indexing, augmented reality and speech analysis. Kurento modular architecture makes simple the integration of third party media processing algorithms (i.e. speech recognition, sentiment analysis, face recognition, etc.), which can be transparently used by application developers as the rest of Kurento built-in features.

For more information about Kurento, please visit [http://www.kurento.org/](http://www.kurento.org/).

# How to use this image

## Start a `kurento-media-server` instance

Starting a Kurento media server instance is easy. Kurento media server exposes port 8888 for client access. So, assuming you want to map port 8888 in the instance to local port 8888, you can start kurento media server with:

```console
$ docker run --name kms -p 8888:8888 -d kurento/kurento-media-server:6.1.0
```

To check that kurento media server is ready and listening, issue the following command (you need to have curl installed on your system):

```console
$ curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Host: 127.0.0.1:8888" -H "Origin: 127.0.0.1" http://127.0.0.1:8888/kurento
```

You will get something like:

```console
1:8888/kurento
HTTP/1.1 500 Internal Server Error
Server: WebSocket++/0.5.1
```

Don't worry about the second line (`500 Internal Server Error`). It's ok, because we are not talking the protocol kurento media server expects, we are just checking that the server is up and listening for connections.

## Kurento media server logs

The kurento media server log is available through the usual way Docker exposes logs for its containers. So assuming you named your container `kms` (with `--name kms` as we did above):

```console
$ docker logs kms
```

## Environment variables

Kurento media server exposes an environment variable `GST_DEBUG` that can be used to set the debug level of kurento media server:

```console
$ docker run -d --name kms -e GST_DEBUG=Kurento*:5 kurento/kurento-media-server:6.1.0
```

## Get help about kurento media server

```console
$ docker run --rm kurento/kurento-media-server:6.1.0 --help
```
