[![][KurentoImage]][website]

Copyright © 2013 Kurento. Licensed under [LGPL License].

kurento-jsonrpc-demo-server
==========

Kurento JsonRpc Demo Server is a demo application of the Kurento JsonRpc 
Server library. It consists of a WebSocket server that includes several 
test handlers of JsonRpc messages.

Installation details
---------------

#### Execute KJRServer 6.0.x

* Build
```sh
cd kurento-jsonrpc-demo-server
mvn clean install
```

* Unzip distribution files
```sh
cd target
unzip kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT.zip
```

* Execute start script
```sh
cd kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT
./bin/start.sh
```

* Configure logging
```sh
vim kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT/config/kjrserver-log4j.properties
```
> Log file by default will be located in kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT/logs/

* Configure server
```sh
vim kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT/config/kjrserver.conf.json
```

#### Start KJRServer 6.0.x as daemon (kjrserver) in Ubuntu or CentOS

* Build
```sh
cd kurento-jsonrpc-demo-server
mvn clean install
```

* Unzip distribution files
```sh
cd target
unzip kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT.zip
```

* Execute install script
```sh
cd kurento-jsonrpc-demo-server-6.0.0-SNAPSHOT
sudo ./bin/install.sh
```
> The service (kjrserver) will be automatically started.

* Control the service (Ubuntu)
```sh
sudo service kjrserver {start|stop|status|restart|reload}
```

* Configure logging
```sh
sudo vim /etc/kurento/kjrserver-log4j.properties
```
> Log file by default will be located in /var/log/kurento/

* Configure server
```sh
sudo vim /etc/kurento/kjrserver.conf.json
```

What is Kurento
---------------
Kurento provides an open platform for video processing and streaming
based on standards.

This platform has several APIs and components which provide solutions
to the requirements of multimedia content application developers.
These include:

  * Kurento Media Server (KMS). A full featured media server providing
    the capability to create and manage dynamic multimedia pipelines.
  * Kurento Control Server (KCS). Signaling server for KMS. It provides
    extra capabilities such as security, load balance, and so on.
  * Kurento Clients. Libraries to create applications with media
    capabilities. Kurento provides libraries for Java, browser JavaScript,
    and Node.js.

Downloads
---------
To download binary releases of Kurento components visit http://kurento.org

Source
------
The source code of this project can be cloned from the [GitHub Repository].
Code for other Kurento projects can be found in the [GitHub Kurento Group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].

[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[GitHub Repository]: https://github.com/Kurento/kurento-java
[GitHub Kurento Group]: https://github.com/kurento
[website]: http://kurento.org
[kurentoms]: http://twitter.com/kurentoms