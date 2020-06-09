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

   mvn -U clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dkms.url=ws://kms_host:kms_port/kurento"

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
   sudo apt-get install --yes nodejs
   sudo npm install -g http-server

* You will need to provide a valid SSL certificate in order to enable HTTPS. There are two alternatives:

  1. Obtain a certificate from a trusted Certification Authority (*CA*).

  2. Create your own untrusted self-signed certificate. You can search articles online that explain how to do this, for example `this one <https://www.akadia.com/services/ssh_test_certificate.html>`__.

     Alternatively, it can be much easier and convenient using a self-signed certificate generation tool, such as `mkcert <https://github.com/FiloSottile/mkcert>`__.

     Note that while a self-signed certificate can be used, browsers will show a big security warning. Users will see this warning, and must click to accept the unsafe certificate before proceeding to the page.

* Start the HTTPS web server, using the SSL certificate:

  .. code-block:: bash

     http-server -p 8443 --ssl --cert keys/server.crt --key keys/server.key



.. _features-security-kms-wss:

Securing Kurento Media Server
=============================

With the default configuration, Kurento Media Server will listen for non-secure WebSocket connections (``ws://``) on the port 8888. Application Servers will establish a WebSocket connection with KMS, in order to control it and send messages conforming to the :doc:`/features/kurento_api`.

This is fine for initial stages of application development, but before deploying on production environments you'll probably want to use Secure WebSocket (``wss://``) connections.

To enable WSS, edit the main KMS configuration file, **/etc/kurento/kurento.conf.json**, and un-comment the following lines:

.. code-block:: text

   "secure": {
     "port": 8433,
     "certificate": "cert+key.pem",
     "password": "KEY_PASSWORD"
   }

If you will be using a signed certificate issued by a trusted Certificate Authority such as Verisign or Let's Encrypt, then you are done. Just skip to the next section: :ref:`features-security-kms-wss-connect`.

However, if you are going to use an untrusted self-signed certificate (typically during development), there is still more work to do.

You can generate a self signed certificate by doing this:

.. code-block:: shell

   certtool --generate-privkey --outfile defaultCertificate.pem

   echo 'organization = your organization name' >certtool.tmpl

   certtool --generate-self-signed --load-privkey defaultCertificate.pem \
      --template certtool.tmpl >>defaultCertificate.pem

   sudo chown kurento defaultCertificate.pem

Alternatively, it is much easier and convenient using a self-signed certificate generation tool, such as `mkcert <https://github.com/FiloSottile/mkcert>`__.

Because self-signed certificates are untrusted by nature, client browsers and server applications will reject it by default. You'll need to force them to accept it:

* **Java applications**: Follow the instructions of `this link <https://www.mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`__ (get ``InstallCert.java`` from `here <https://code.google.com/p/java-use-examples/source/browse/trunk/src/com/aw/ad/util/InstallCert.java>`__).

  You'll need to instruct the ``KurentoClient`` to allow using certificates. For this purpose, create an ``JsonRpcClient``:

.. code-block:: java

   SslContextFactory sec = new SslContextFactory(true);
   sec.setValidateCerts(false);
   JsonRpcClientWebSocket rpcClient = new JsonRpcClientWebSocket(uri, sec);
   KurentoClient kurentoClient = KurentoClient.createFromJsonRpcClient(rpcClient);

* **Node applications**: Take a look at this page: `Painless Self Signed Certificates in node.js <https://git.coolaj86.com/coolaj86/ssl-root-cas.js/src/branch/master/Painless-Self-Signed-Certificates-in-node.js.md>`__.

* **Browser JavaScript applications**: Similar to what happens with self-signed certificates used for HTTPS, browsers also require the user to accept a security warning before Secure WebSocket connections can be established. This is done by directly opening the KMS WebSocket URL: https://KMS_HOST:8433/kurento



.. _features-security-kms-wss-connect:

Connecting to a secured KMS
---------------------------

Now that KMS is listening for Secure WebSocket connections, and (if using a self-signed certificate) your Application Server is configured to accept the certificate used in KMS, you have to change the WebSocket URL used in your application logic.

Make sure your application uses a WebSocket URL that starts with ``wss://`` instead of ``ws://``. Depending on the platform, this is done in different ways:

* **Java**: Launch with a ``kms.url`` property. For example:

  .. code-block:: java

     mvn clean spring-boot:run -Dkms.url="wss://KMS_HOST:8433/kurento"

* **Node.js**: Launch with the ``ws_uri`` command-line argument. For example:

  .. code-block:: js

     npm start -- --ws_uri="wss://KMS_HOST:8433/kurento"

* **Browser JavaScript**: Application-specific method. For example, using hardcoded values:

  .. code-block:: js

     const ws_uri: "wss://" + location.hostname + ":8433/kurento";
