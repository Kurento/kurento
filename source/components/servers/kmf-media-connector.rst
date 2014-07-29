KMF Media Connector
===================

.. highlight:: console

The KMF Media Connector is a proxy that allows to clients to connect to
Kurento Media Server through websockets. The main Kurento Media Server
interface is based on thrift technology, and this proxy made necessary
convertions between websockets and thrift.

Installation instructions
-------------------------

This proxy can be installed in any node with full network connectivity
to the Kurento Media Server. That is, the Kurento Media Server has to be
installed in a node with a network address and port reachable from the
proxy and the proxy has to be installed in a node with a network address
and port reachable from the Kurento Media Server. For this restrictions,
it is easier to install the proxy in the same node than Kurento Media Server.

The proxy is implemented with Java 7 technology. Java 7 is the only
prerequisite of the proxy. There is no need to install additional software.
The proxy is provided as a .jar Java executable archive, and has an embedded
Tomcat server.

Download the KMF Media Connector and move it to
``/opt/kmf-media-connector`` by executing::

    sudo wget http://jmaster01-64.kurento.com/apps/kmf-media-connector.zip
    sudo mkdir /opt/kmf-media-connector
    sudo mv kmf-media-connector.zip /opt/kmf-media-connector

Unzip the kmf-media-connector.zip file::

    cd /opt/kmf-media-connector
    sudo unzip kmf-media-connector-|version|.zip

Move the init.d script to the right location and give the correct permission
to the file::

    sudo cp /opt/kmf-media-connector/support-files/kmf-media-connector.sh\
        /etc/init.d/kmf-media-connector
    chmod 755 /etc/init.d/kmf-media-connector

Give the correct permissions to the start.sh file. This file is used by the
kmf-media-connector.sh service script::

    chmod 755 /opt/kmf-media-connector/bin/start.sh

Finally, configure the server to run kmf-media-connector when booted::

    sudo update-rc.d kmf-media-connector defaults

And run the kmf-media-connector proxy service:

    sudo service kmf-media-connector start

Alternatively, you can run the KMF Media Connector passing the arguments
directly from the command line. Assuming that the command ``java`` points
to a Java 7 JVM executable, the proxy can be executed as::

    java -jar kmf-media-connector.jar --server.port=8080\
    > --mediaserver.address=127.0.0.1 --mediaserver.port=9090\
    > --handler.address=127.0.0.1 --handler.port=9900\
    > --oauthserver.url=http://cloud.lab.fi-ware.org 

Description of the configuration properties:

``server.port``
    The http/websocket port of the proxy. This port will be
    used for the clients to connect to the port. If not specified, the
    value 8080 will be used.

``mediaserver.address``
    The IP or address of the Kurento Media Server.
    If not specified, the address 127.0.0.1 will be used.

``mediaserver.port``
    The port of the thrift interface in Kurento Media Server.
    If not specified, the port 9090 will be used.

``handler.address``
    The IP or address that Kurento Media Server will use to connect
    to the proxy. If not specified, the address 127.0.0.1 will be used.

``handler.port``
    The port that Kurento Media Server will use to connect to the proxy.
    If not specified, the port 9900 will be used.

``oauthserver.url``
    The url of the oauth service used to authenticate the client requests.
    The url ``http://cloud.lab.fi-ware.org`` is the official OAuth service
    in FI-WARE project. The empty URL can be used to allow all clients to
    use the proxy (that is, no authentication is enforced). If not specified,
    the empty URL will be used.

If you prefer, insted of using the command line parameters, you can specify
these properties in a configuration file. The configuration file has to
be called ``application.properties`` and must be placed in the working
directory. The file is formated as a plain Java properties file:

.. sourcecode:: properties

    server.port=80
    mediaserver.address=127.0.0.1
    mediaserver.port=9090
    handler.address=127.0.0.1
    handler.port=9900
    oauthserver.url=http://cloud.lab.fi-ware.org

If you don't like application.properties as the configuration file name
you can switch to another by specifying ``spring.config.name`` environment
property.::

    $ java -jar myproject.jar --spring.config.name=myproject

You can also refer to an explicit location using the spring.config.location
environment property::

    $ java -jar myproject.jar --spring.config.location=/opt/kmfmediaconnector

It is recommended to wrap the proxy in system service in order to be
executed when the system starts. 

The log of the proxy can be configured. You can specify the property
``logging.config`` pointing to a file configurating the Logback system.
The value of this property can be specified in command line or in the
``application.properties`` file. 

The possible configuration of the Logback system is out of the scope of
this manual and can be consulted in
http://logback.qos.ch/manual/configuration.html.
