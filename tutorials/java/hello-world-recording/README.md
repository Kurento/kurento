kurento-hello-world-recording
=============================

Kurento Java Tutorial: Hello World (WebRTC in loopback) with recording.

Running the tutorial
--------------------

In order to run this tutorial, please read the following [instructions](https://kurento.openvidu.io/docs/current/tutorials/java/tutorial-recording.html).

After cloning the tutorial, it can be executed directly from the terminal by
using Maven's `exec` plugin (`[...]` is optional):

```
$ git clone git@github.com:Kurento/kurento-tutorial-java.git
$ cd kurento/tutorials/java/hello-world-recording/
$ mvn -U clean spring-boot:run [-Dkms.url=ws://localhost:8888/kurento]
```

### Dependencies ###

If using a *SNAPSHOT* version (e.g. latest commit from **main** branch), the
project `kurento-java` is also required to exist (built and installed) in the
local Maven repository.

```
$ git clone git@github.com:Kurento/kurento-java.git
$ cd kurento/clients/java/
$ mvn clean install -DskipTests -Pdefault
```

