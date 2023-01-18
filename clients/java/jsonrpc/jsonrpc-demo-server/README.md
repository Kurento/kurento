kurento-jsonrpc-demo-server
===========================

Kurento JsonRpc Demo Server is a demo application of the Kurento JsonRpc
Server library. It consists of a WebSocket server that includes several
test handlers of JsonRpc messages.

Installation details
---------------

#### Execute KJRServer

* Build
```sh
git clone git@github.com:Kurento/kurento-java.git
cd kurento/clients/java/jsonrpc/jsonrpc-demo-server
git checkout $(git describe --abbrev=0 --tags)
mvn clean install
```

* Unzip distribution files
```sh
cd target
unzip kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT.zip
```

* Execute start script
```sh
cd kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT
./bin/start.sh
```

* Configure logging
```sh
vim kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT/config/kjrserver-log4j.properties
```
> Log file by default will be located in kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT/logs/

* Configure server
```sh
vim kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT/config/kjrserver.conf.json
```

#### Start KJRServer as daemon (kjrserver) in Ubuntu or CentOS

* Build
```sh
git clone git@github.com:Kurento/kurento-java.git
cd kurento/clients/java/jsonrpc/jsonrpc-demo-server
git checkout $(git describe --abbrev=0 --tags)
mvn clean install
```

* Unzip distribution files
```sh
cd target
unzip kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT.zip
```

* Execute install script
```sh
cd kurento-jsonrpc-demo-server-x.y.z-SNAPSHOT
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

