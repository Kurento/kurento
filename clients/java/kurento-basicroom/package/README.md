[![License badge](https://img.shields.io/badge/license-Apache2-orange.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Documentation badge](https://readthedocs.org/projects/fiware-orion/badge/?version=latest)](http://doc-kurento.readthedocs.org/en/latest/)
[![Docker badge](https://img.shields.io/docker/pulls/fiware/orion.svg)](https://hub.docker.com/r/fiware/stream-oriented-kurento/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/kurento)

Kurento Room Server
======================

The Kurento Room Server (KRS) is a Kurento Server component that provides a
service for creating WebRTC group communications apps.

Installation instructions
-------------------------

KRS is implemented with Java 7 technology and Java 7 is the only
prerequisite. The KRS is provided as a .zip containing a Java executable archive
(.jar).

Assuming that the command 'java' points to a Java 7 JRE executable, KCS
can be executed as:

    java -jar kurento-room-server.jar

All configuration is retrieved from file 'kurento-room.conf.json' located in config
folder in the place you are executing java command.

{
   "ws": {
     "port": "8891",
     "path": "kurento-room"
   },
   "kms": {
      "uri": "ws://<ip-of-server1>:<port-of-server1>/kurento"
   }
}

Also, the configuration keys can be overridden with Java system properties. For
example, if you want to override 'ws.port', you have to execute
the following command:

    java -Dws.port=8888 -jar kurento-room-server.jar

Configuration options
----

The meaning of general configuration properties are:

**WebSocket interface**

* **ws.port:** The http/websocket port of KRS. This port
  will be used for clients to connect to Kurento Room Server. If not specified,
  the value 8891 will be used.
* **ws.path:** The websocket relative path of KRS. If not
  specified, the relative path will be 'kurento-room'.
* **kms.uri:** The websocket uri where the KMS is available


Custom location of configuration file
------------------

You can change the location and name of the configuration file with the
following command:

    java -DconfigFilePath=/opt/kurento/config.json -jar kurento-room-server.jar
