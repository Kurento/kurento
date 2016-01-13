.. _securingapps:

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Securing Kurento Applications
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Starting with Chrome 47, WebRTC is only allowed from SECURE ORIGINS (HTTPS or localhost).
Check their `release notes <https://groups.google.com/forum/#!topic/discuss-webrtc/sq5CVmY69sc>`_
for further information about this issue.

.. note::

      Keep in mind that serving your application through HTTPS, forces you to use WebSockets Secure (WSS)
      if you are using websockets to control your application server.

Securing client applications
============================

Configure Java applications to use HTTPS
----------------------------------------


* The application needs a certificate in order to enable HTTPS:

   * Request a certificate from a local certification authority.

   * Create an self-signed certificate.

      .. sourcecode:: bash

         keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048


* Use the certificate in your application:

     * Include a valid keystore in the *jar* file:

        File *keystore.jks* must be in the project's root path, and a file 
        named *application.properties* must exist in *src/main/resources/*, 
        with the following content:

         .. sourcecode:: bash

            server.port: 8443
            server.ssl.key-store: keystore.jks
            server.ssl.key-store-password: yourPassword
            server.ssl.keyStoreType: JKS
            server.ssl.keyAlias: yourKeyAlias

      * You can also specify the location of the properties file. Just issue the flag `-Dspring.config.location=<path-to-properties>` when launching your Spring-Boot based app. 

* Start application

.. sourcecode:: bash

   mvn compile exec:java -Dkms.ws.uri=ws://kms_host:kms_port/kurento

.. note::

      If you plan on using a webserver as proxy, like Nginx or Apache, you'll need to ``setAllowedOrigins`` when registering the handler. Please read the `official Spring documentation <http://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-server-allowed-origins>`_ entry for more info.



Configure Node applications to use HTTPS
----------------------------------------

* The application requires a valid SSL certificate in order to enable HTTPS:

   * Request a certificate from a local certification authority.

   * Create your own self-signed certificate as explained `here <http://www.akadia.com/services/ssh_test_certificate.html>`_. This will show you how to create the required files: *server.crt*, *server.key* and *server.csr*.

Add the following changes to *server.js* in order to enable HTTPS:

.. sourcecode:: javascript

   ...
   var express = require('express');
   var ws = require('ws');
   var fs    = require('fs');
   var https = require('https');
   ...

   var options =
   {
     key:  fs.readFileSync('key/server.key'),
     cert: fs.readFileSync('keys/server.crt')
   };

   var app = express();

   var server = https.createServer(options, app).listen(port, function() {
   ...
   });
   ...

   var wss = new ws.Server({
    server : server,
    path : '/'
   });

   wss.on('connection', function(ws) {

   ....

* Start application

.. sourcecode:: bash

   npm start

Configure Javascript applications to use HTTPS
----------------------------------------------

* You'll need to provide a valid SSL certificate in order to enable HTTPS:

   * Request a certificate from a local certification authority.

   * Create your own self-signed certificate as explained `here <http://www.akadia.com/services/ssh_test_certificate.html>`_. This will show you how to create the required files: *server.crt*, *server.key* and *server.csr*.


* Start the application using the certificates:

.. sourcecode:: bash

   http-server -p 8443 -S -C keys/server.crt -K keys/server.key


Securing server applications
============================

Configure Kurento Media Server to use Secure WebSocket (WSS)
------------------------------------------------------------

First, you need to change the configuration file of Kurento Media Server,
i.e. ``/etc/kurento/kurento.conf.json``, uncommenting the following lines::

   "secure": {
     "port": 8433,
     "certificate": "defaultCertificate.pem",
     "password": ""
   },

You will also need a PEM certificate that should be in the same path of
the configuration file, or you may need to specify the full path in the ``certificate``
field. Take into account that this file must contain the entire trust chain. If you have
several files, you probably need to concatenate the content of those files
in order to obtain a valid certificate bundle. Assuming that the names correspond to each kind of
certificate that you might have, the following commnad will create a valid SSL certificate
chain bundle::

   $ cat signing-ca.crt subordinate-ca.crt server.crt > server.pem

The file ``server.pem`` is the file that you will need to point to in the configuration
file.

Second, you have to change the WebSocket URI in your application logic. For
instance, in the *hello-world* application within the tutorials, this would
be done as follows:

- Java: Changing this line in `HelloWorldApp.java <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/java/org/kurento/tutorial/helloworld/HelloWorldApp.java>`_::

   final static String DEFAULT_KMS_WS_URI = "wss://localhost:8433/kurento";

- Browser JavaScript: Changing this line in `index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-hello-world/js/index.js>`_::

    const ws_uri = 'wss://' + location.hostname + ':8433/kurento';

- Node.js: Changing this line in `server.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/server.js>`_::

   const ws_uri = "wss://localhost:8433/kurento";

If this PEM certificate is a signed certificate (by a Certificate Authority such
as Verisign), then you are done. If you are going to use a self-signed certificate
(suitable for development), then there is still more work to do.

You can generate a self signed certificate by doing this::

   certtool --generate-privkey --outfile defaultCertificate.pem
   echo 'organization = your organization name' > certtool.tmpl
   certtool --generate-self-signed --load-privkey defaultCertificate.pem \
      --template certtool.tmpl >> defaultCertificate.pem
   sudo chown nobody defaultCertificate.pem

Due to the fact that the certificate is self-signed, applications will reject it
by default. For this reason, you'll need to force them to accept it.

* Browser applications: You'll need to manually accept the certificate as trusted one before secure webscoket connections can be stablished.

* Java applications, follow the instructions of this `link <http://www.mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`_ (get ``InstallCert.java`` from `here <https://code.google.com/p/java-use-examples/source/browse/trunk/src/com/aw/ad/util/InstallCert.java>`__). You'll need to instruct the ``KurentoClient`` needs to be configured to allow the use of certificates. For this purpose, we need to create our own ``JsonRpcClient``::

   SslContextFactory sec = new SslContextFactory(true);
   sec.setValidateCerts(false);
   JsonRpcClientWebSocket rpcClient = new JsonRpcClientWebSocket(uri, sec);
   KurentoClient kuretoClient = KurentoClient.createFromJsonRpcClient(rpcClient);

* Node applications, please take a look to this
  `page <https://github.com/coolaj86/node-ssl-root-cas/wiki/Painless-Self-Signed-Certificates-in-node.js>`_.