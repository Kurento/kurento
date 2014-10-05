Kurento Control Server
======================

The Kurento Control Server (KCS) is a Kurento Server component depicted to allow
clients to connect to a distributed Kurento Media Server (KMS) through RabbitMQ
message broker.

Installation instructions
-------------------------

KCS is implemented with Java 7 technology and Java 7 is the only
prerequisite. The KCS is provided as a .zip containing a Java executable archive
(.jar).

Assuming that the command 'java' points to a Java 7 JRE executable, KCS
can be executed as:

    java -jar kurento-control-server.jar

All configuration is retrieved from file 'kurento.conf.json' located in config
folder in the place you are executing java command.

This file looks like:

    {
       "mediaServer":{
        "net":{
          // Uncomment just one of them
          "websocket": {
            "port": 8888,
            "path": /kurento,
            "threads": 10
          }
          // "rabbitmq":{
          //   "host":"127.0.0.1",
          //   "port":5672,
          //   "username":"guest",
          //   "pass":"guest",
          //   "vhost" : "/"
          // }
          }
      },
      "controlServer" : {
        "net" : {
            "websocket" : {
                "port": 8888,
                "path": "kurento",
                //"securePort": 8433
                //"keystore" : {
                //   "path" : "/path/to/file",
                //   "pass" : "changeme"
                //}
            }
         },
         // "oauthserverUrl": "",
         // "logConfigFile": "/opt/kurento/logback.xml",
         // "unsecureRandom": false
      }
    }

Also, the configuration keys can be overridden with Java system properties. For
example, if you want to override 'controlServer.net.websocket.port', you have to execute
the following command:

    java -DcontrolServer.net.websocket.port=8888 -jar kurento-control-server.jar

Configuration options
----

The meaning of general configuration properties are:

**WebSocket interface**

* **controlServer.net.websocket.port:** The http/websocket port of KCS. This port
  will be used for clients to connect to Kurento Server. If not specified, the
  value 8888 will be used.
* **controlServer.net.websocket.path:** The websocket relative path of KCS. If not
  specified, the relative path will be 'kurento'.
* **controlServer.net.websocket.securePort:** The http/websocket secure port of KCS.
  This port will be used for clients to connect to Kurento Server thought secure
  websocket connection (wss). If not specified, no secure connection will be
  allowed.
* **controlServer.net.websocket.keystore.path:** The path to the keystore file
  with the private key used when securePort is specified. This keystore file has
  to have a private key with 'kurento' alias. To generate this file it is
  recommended to have a real private key and certificate file
  (not a self-signed certificate). With this files, you can use the following
  command to generate the needed file:

     openssl pkcs12 -export -in server.crt -inkey server.key -out keystore -name kurento

  If you want to use a self-signed certificate you can generate the keystore file
  with a new private key and a self-signed certificate with the command:

    keytool -genkey -alias kurento -storetype PKCS12 -keystore keystore

* **controlServer.net.websocket.keystore.pass:** The keystore file password. This password is
  specified interactively when executing any of previous commands.

**RabbitMQ interface**

* **mediaServer.net.rabbitmq.host:** Specifies the host name of the RabbitMQ broker.
  The default value is 'localhost'.
* **mediaServer.net.rabbitmq.port:** Specifies the port of the RabbitMQ broker.
  The default value is 5672.
* **mediaServer.net.rabbitmq.username:** Specifies the username used to connect to
  RabbitMQ broker. The default value is 'guest'.
* **mediaServer.net.rabbitmq.pass:** Specifies the password used to connect to
  RabbitMQ broker. The default value is 'guest'.
* **mediaServer.net.rabbitmq.vhost:** Specifies the virtual host used in RabbitMQ
  broker. The default value is '/'.

**Other properties**

* **controlServer.logConfigFile:** Log config file path. KCS log is based on
  Logback, a famous log system in Java. I you want to configure the log, you have
  to provide a custom log configuration file and specify its location in this
  property. You can found more information about logback in
  http://logback.qos.ch/manual/configuration.html.
* **controlServer.oauthserver.url:** The url of the oauth service used to
  authenticate the client requests. The empty URL can be used to allow all
  clients to use KCS (that is, no authentication is enforced). If not specified,
  the empty URL will be used.
* **controlServer.unsecureRandom:** Use /dev/urandom for secure random
  generation. This can be useful in systems with low entropy (such as headless
  virtual machines). You can found more info in the following pages:

  * http://www.shilpikhariwal.com/2012/04/random-number-generation-in-unix.html
  * https://www.digitalocean.com/community/tutorials/how-to-setup-additional-entropy-for-cloud-servers-using-haveged


Custom location of configuration file
------------------

You can change the location and name of the configuration file with the
following command:

    java -DconfigFilePath=/opt/kurento/config.json -jar kurento-control-server.jar