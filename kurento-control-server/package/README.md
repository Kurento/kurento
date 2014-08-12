Kurento Control Server
======================

The Kurento Control Server (KCS) is a Kurento Server component depicted to allow
clients to connect to Kurento Media Server (KMS) through web sockets.
Kurento Media Server supports two transport technologies: Thrift or RabbitMQ.
This component made necessary conversions between websockets and Thrift or
RabbitMQ.

Installation instructions
-------------------------

KCS is implemented with Java 7 technology and Java 7 is the only
prerequisite. The KCS is provided as a .zip containing a Java executable archive
(.jar).

KCS can be configured to connect to KMS with Thrift or
by means of RabbitMQ. Thrift is the default choice because it is easier to get
the whole system working. Please refer to Kurento Media Server documentation
for advantages and disadvantages in using Thrift vs RabbitMQ.

Assuming that the command 'java' points to a Java 7 JRE executable, KCS
can be executed as:

    java -jar kurento-control-server.jar

All configuration is retrieved from file kurento.conf.json located in config
folder in the place of executing java command.

This file looks like:

    {
      "mediaServer":{
        "net":{
          "thrift":{
            "port":9090
          }
	      }
	    },
	    "controlServer":{
	      "net": {
	        "websocket" : {
	          "port": 8888,
	          "path": "kurento"
	        },
	        "thriftCallback": {
	          "port": 9900
          }
        }
      }
    }

Also, the configuration keys can be overridden with Java system properties. For
example, if you want to override 'thriftCallback' 'port', you have to execute
the following command:

    java -DcontrolServer.net.thriftCallback.port=7777 -jar kurento-control-server.jar

Generical configuration options
-------------------------------

The meaning of general configuration properties are:

* **controlServer.net.websocket.port:** The http/websocket port of KCS. This port
  will be used for clients to connect to Kurento Server. If not specified, the
  value 8888 will be used.
* **controlServer.net.websocket.path:** The websocket relative path of KCS. If not
  specified, the relative path will be 'kurento'.
* **controlServer.net.websocket.securePort:** The http/websocket secure port of KCS.
  This port will be used for clients to connect to Kurento Server thought secure
  websocket connection (wss). If not specified, no secure connection will be
  allowed.
* **controlServer.oauthserver.url:** The url of the oauth service used to
  authenticate the client requests. The empty URL can be used to allow all
  clients to use KCS (that is, no authentication is enforced). If not specified,
  the empty URL will be used.
* **controlServer.keystoreFile:** The keystore file with the private key used when
  securePort is specified. This keystore file has to have a private key with
  'tomcat' alias. To generate this file it is recommended to have a real private
  key and certificate file (not a self-signed certificate). With this files,
  you can use the following command to generate the needed file:

     openssl pkcs12 -export -in server.crt -inkey server.key -out keystore -name tomcat

  If your client it is not a web browser, maybe you can use a self-signed
  certificate. It depends on client websocket library that you use to connect to
  KCS. If this works to you, you can generate the keystore file with a
  new private key and a self-signed certificate with the command:

    keytool -genkey -alias tomcat -storetype PKCS12 -keystore keystore

* **controlServer.keystorePass:** The keystore file password. This password is
  specified interactively when executing any of previous commands.


Thrift transport
----------------

With Thrift transport KCS has to be installed in a node with full network
connectivity with the KMS. That is, KMS
has to be installed in a node with a network address and port reachable from
KCS and KCS has to be installed in a node with a network address and
port reachable from the KMS. For this restrictions, it is more
easy to install KCS and KMS in the same host.

* **mediaServer.net.thrift.host:** The host of KMS. If not specified, the host will
  be 'localhost'.
* **mediaServer.net.thrift.port:** The port of thrift interface of KMS. If not
  specified, the port will be 9090.
* **controlServer.net.thriftCallback.host:** The host name that KMS will use
  to connect KCS. If not specified, the host name will be 'localhost'.
* **controlServer.net.thriftCallback.port:** The port that KCS will open to receive
  thrift callbacks from KMS. If not specified, 9900 will be used.

RabbitMQ transport
------------------

The meaning of configuration properties are:

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

With this properties, the configuration file will look like:

    {
      "mediaServer":{
        "net":{
          "rabbitmq":{
            "host":"127.0.0.1",
            "port":5672,
            "username":"guest",
            "pass":"guest",
            "vhost" : "/"
          }
        }
      },
      "controlServer":{
        "net": {
          "websocket" : {
            "port": 8888,
            "path": "kurento"
          }
        }
      }
    }


Configuration file
------------------

If you prefer, instead of using the command line parameters, you can specify the
configuration properties in a configuration file. The configuration file has to
be called 'application.properties' and must be placed in the working directory.
The file is formated as a plain Java properties file, for example:

server.port=8888
thrift.kms.address=127.0.0.1:9090
thrift.kmf.address=127.0.0.1:9191

If you don't like application.properties as the configuration file name you can
switch to another by specifying spring.config.location environment property.

$ java -jar kmf-media-connector.jar --spring.config.location=newconfigfilename

You can also refer to an explicit location using the spring.config.location
environment property.

$ java -jar kmf-media-connector.jar --spring.config.location=/opt/kmfmediaconnector

Notice that in both cases, the configuration file must be a valid properties file,
i.e. a file with extension .properties (e.g. application.properties).

It is recommended to wrap the proxy in system service in order to be executed
when the system starts.

The log of the proxy can be configured. You can specify the property
'logging.config' pointing to a file to configure the Logback system. The value
of this property can be specified in command line or in the
application.properties file.

The possible configuration of the Logback system is out of the scope of this
manual and can be consulted in http://logback.qos.ch/manual/configuration.html.
