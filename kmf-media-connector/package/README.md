KMF Media Connector
===================

The KMF Media Connector is a proxy that allows to clients to connect to Kurento
 Media Server through websockets. Kurento Media Server supports two transport 
 technologies: Thrift or RabbitMQ. This proxy made necessary conversions
 between websockets and Thrift or RabbitMQ.

Installation instructions
-------------------------

The proxy is implemented with Java 7 technology. Java 7 is the only 
prerequisite of the proxy. There is no need to install additional software. 
The proxy is provided as a .jar Java executable archive, and has an embedded 
Tomcat server.

The proxy can be configured to connect to Kurento Media Server with Thrift or 
by means of RabbitMQ. Thrift is the default choice because it is easier to get 
the whole system working. Please refer to Kurento Media Server documentation 
for advantages and disadvantages in using Thrift vs RabbitMQ.

Thrift transport
----------------

With Thrift transport the proxy has to be installed in a node with full network 
connectivity with the Kurento Media Server. That is, the Kurento Media Server 
has to be installed in a node with a network address and port reachable from 
the proxy and the proxy has to be installed in a node with a network address and 
port reachable from the Kurento Media Server. For this restrictions, it is more 
easy to install the proxy in the same node than Kurento Media Server.

Assuming that the command 'java' points to a Java 7 JVM executable, the proxy 
can be executed as:

$ java -jar kmf-media-connector.jar --server.port=8888 
  --thrift.kms.address=127.0.0.1:9090
  --thrift.kmf.address=127.0.0.1:9191

The meaning of configuration properties are:
* server.port: The http/websocket port of the proxy. This port will be used for 
  the clients to connect to the port. If not specified, the value 8888 will be 
  used.
* thrift.kms.address: The IP and port address of the Kurento Media Server. 
  If not specified, the address 127.0.0.1:9090 will be used.
* thrift.kmf.address: The IP and port address that Kurento Media Server will use
  to connect to the proxy. If not specified, the address 127.0.0.1:9090 will be 
  used.


RabbitMQ transport
------------------

If you prefer to use RabbitMQ to connect to Kurento Media Server, the proxy
has to have access to a previously running RabbitMQ server. Please refer to
RabbitMQ documentation for this.

Assuming that the command 'java' points to a Java 7 JVM executable, the proxy 
can be executed as:

$ java -jar kmf-media-connector.jar --server.port=8888 --kmf.transport=rabbitmq 
  --rabbitmq.address=127.0.0.1:5672

The meaning of configuration properties are:
* server.port: The http/websocket port of the proxy. This port will be used for 
  the clients to connect to the port. If not specified, the value 8888 will be 
  used.
* rabbitmq.address: Specifies the address of the RabbitMQ broker. The default 
  value is "127.0.0.1:5672".

Other configuration properties
------------------------------

There are other common configuration properties regarding to transport used: 

* oauthserver.url: The url of the oauth service used to authenticate the client 
  requests. The url "http://cloud.lab.fi-ware.org" is the official OAuth service
  in FI-WARE project. The empty URL can be used to allow all clients to use the 
  proxy (that is, no authentication is enforced). If not specified, 
  the empty URL will be used.


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
