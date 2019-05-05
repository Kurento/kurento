=============================
Securing Kurento Applications
=============================

[TODO full review]

Starting with Chrome 47, WebRTC is only allowed from SECURE ORIGINS (HTTPS or localhost). Check their `release notes <https://groups.google.com/forum/#!topic/discuss-webrtc/sq5CVmY69sc>`__ for further information about this issue.

.. note::

   Keep in mind that serving your application through HTTPS, forces you to use WebSockets Secure (WSS) if you are using websockets to control your application server.



Securing Application Servers
============================

.. _features-security-java-https:

Configure a Java server to use HTTPS
------------------------------------

* The application needs a certificate in order to enable HTTPS:

   * Request a certificate from a local certification authority.

   * Create an self-signed certificate.

     .. sourcecode:: bash

        keytool -genkey -keyalg RSA -alias selfsigned -keystore \
        keystore.jks -storepass password -validity 360 -keysize 2048

* Use the certificate in your application:

  * Include a valid keystore in the *jar* file:

    A file *keystore.jks* must be in the project's root path, and a file named *application.properties* must exist in *src/main/resources/*, with the following content:

    .. sourcecode:: text

       server.port: 8443
       server.ssl.key-store: keystore.jks
       server.ssl.key-store-password: yourPassword
       server.ssl.keyStoreType: JKS
       server.ssl.keyAlias: yourKeyAlias

    * You can also specify the location of the properties file. When launching your Spring-Boot based app, issue the flag ``-Dspring.config.location=<path-to-properties>`` .

* Start application

.. sourcecode:: bash

   mvn -U clean spring-boot:run -Dkms.url=ws://kms_host:kms_port/kurento

.. note::

   If you plan on using a webserver as proxy, like Nginx or Apache, you'll need to ``setAllowedOrigins`` when registering the handler. Please read the `official Spring documentation <https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-server-allowed-origins>`__ entry for more info.



.. _features-security-node-https:

Configure a Node server to use HTTPS
------------------------------------

* You will need to provide a valid SSL certificate in order to enable HTTPS. Here, there are two alternatives:

  1. Request a certificate from a local Certification Authority (*CA*).

  2. Create your own self-signed certificate as explained `here <https://www.akadia.com/services/ssh_test_certificate.html>`__. This link will teach you how to create the required files: *server.crt*, *server.key*, and *server.csr*.

* Add the following changes to *server.js* in order to enable HTTPS:

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



.. _features-security-js-https:

Configure JavaScript applications to use HTTPS
----------------------------------------------

WebRTC requires HTTPS, so your JavaScript application must be served by a secure web server. You can use whichever one you prefer, such as Nginx or Apache. For quick tests, a very straightforward option is to use the simple, zero-configuration `http-server <https://www.npmjs.com/package/http-server>`__ based on Node.js:

.. code-block:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g http-server

* You will need to provide a valid SSL certificate in order to enable HTTPS. Here, there are two alternatives:

  1. Request a certificate from a local Certification Authority (*CA*).

  2. Create your own self-signed certificate as explained `here <https://www.akadia.com/services/ssh_test_certificate.html>`__. This link will teach you how to create the required files: *server.crt*, *server.key*, and *server.csr*.

* Start the web server using the SSL certificate:

  .. code-block:: bash

     http-server -p 8443 --ssl --cert keys/server.crt --key keys/server.key



.. _features-security-kms-wss:

Securing Kurento Media Server
=============================

First, you need to change the configuration file of Kurento Media Server, i.e.
``/etc/kurento/kurento.conf.json``, uncommenting the following lines::

   "secure": {
     "port": 8433,
     "certificate": "defaultCertificate.pem",
     "password": ""
   },

If this PEM certificate is a signed certificate (by a Certificate Authority such
as Verisign), then you are done. If you are going to use a self-signed
certificate (suitable for development), then there is still more work to do.

You can generate a self signed certificate by doing this::

   certtool --generate-privkey --outfile defaultCertificate.pem
   echo 'organization = your organization name' > certtool.tmpl
   certtool --generate-self-signed --load-privkey defaultCertificate.pem \
      --template certtool.tmpl >> defaultCertificate.pem
   sudo chown kurento defaultCertificate.pem

Due to the fact that the certificate is self-signed, applications will reject it
by default. For this reason, you'll need to force them to accept it.

* Browser applications: You'll need to manually accept the certificate as
  trusted one before secure WebSocket connections can be established. By
  default, this can be done by connecting to https://localhost:8433/kurento
  and accepting the certificate in the browser.

* Java applications: Follow the instructions of `this link <https://www.mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`__
  (get ``InstallCert.java`` from
  `here <https://code.google.com/p/java-use-examples/source/browse/trunk/src/com/aw/ad/util/InstallCert.java>`__).
  You'll need to instruct the ``KurentoClient`` needs to be configured to allow
  the use of certificates. For this purpose, we need to create our own
  ``JsonRpcClient``:

.. sourcecode:: java

   SslContextFactory sec = new SslContextFactory(true);
   sec.setValidateCerts(false);
   JsonRpcClientWebSocket rpcClient = new JsonRpcClientWebSocket(uri, sec);
   KurentoClient kuretoClient = KurentoClient.createFromJsonRpcClient(rpcClient);

* Node applications: Take a look at `this page <https://github.com/coolaj86/node-ssl-root-cas/wiki/Painless-Self-Signed-Certificates-in-node.js>`__.

After having configured the certificate in your Application Server, you have to change the WebSocket URI in your application logic, and make sure the WebSocket URL starts with ``wss://`` instead of the insecure version ``ws://``. For instance, in the *hello-world* application within the tutorials, this would be done as follows:

* Java: Changing this line in
  `HelloWorldApp.java <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/java/org/kurento/tutorial/helloworld/HelloWorldApp.java>`__::

   final static String DEFAULT_KMS_WS_URI = "wss://localhost:8433/kurento";

* Browser JavaScript: Changing this line in
  `index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-hello-world/js/index.js>`__::

   const ws_uri = 'wss://' + location.hostname + ':8433/kurento';

* Node.js: Changing this line in
  `server.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/server.js>`__::

   const ws_uri = "wss://localhost:8433/kurento";

* All: Passing the WebSocket URL to the Application as a startup parameter (see each individual tutorial page to get the syntax for doing so).
