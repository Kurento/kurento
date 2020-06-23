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

* Obtain a certificate. For this, either request one from a trusted Certification Authority (*CA*), or generate your own one as explained here: :ref:`features-security-selfsigned`.

* Convert your PEM certificate to either `Java KeyStore <https://en.wikipedia.org/wiki/Java_KeyStore>`__ (*JKS*) or `PKCS#12 <https://en.wikipedia.org/wiki/PKCS_12>`__. The former is a proprietary format limited to the Java ecosystem, while the latter is an industry-wide used format. To make a PKCS#12 file from an already existing PEM certificate, run these commands:

  .. code-block:: console

     openssl pkcs12 \
         -export \
         -in cert.pem -inkey key.pem \
         -out cert.p12 -passout pass:123456

     chmod 440 *.p12

* Use the certificate in your application.

  Place your PKCS#12 file *cert.p12* in ``src/main/resources/``, and add this to the *application.properties* file:

  .. code-block:: properties

     server.port=8443
     server.ssl.key-store=classpath:cert.p12
     server.ssl.key-store-password=123456
     server.ssl.key-store-type=PKCS12

* Start the Spring Boot application:

  .. code-block:: console

     mvn -U clean spring-boot:run \
         -Dspring-boot.run.jvmArguments="-Dkms.url=ws://{KMS_HOST}:8888/kurento"

.. note::

   If you plan on using a webserver as proxy, like Nginx or Apache, you'll need to ``setAllowedOrigins`` when registering the handler. Please read the `official Spring documentation <https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-server-allowed-origins>`__ entry for more info.



.. _features-security-node-https:

Configure a Node server to use HTTPS
------------------------------------

* Obtain a certificate. For this, either request one from a trusted Certification Authority (*CA*), or generate your own one as explained here: :ref:`features-security-selfsigned`.

* Add the following changes to your *server.js*, in order to enable HTTPS:

  .. sourcecode:: javascript

     ...
     var express = require('express');
     var ws      = require('ws');
     var fs      = require('fs');
     var https   = require('https');
     ...

     var options =
     {
       cert: fs.readFileSync('cert.pem'),
       key:  fs.readFileSync('key.pem'),
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

.. code-block:: console

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install --yes nodejs
   sudo npm install -g http-server

* Obtain a certificate. For this, either request one from a trusted Certification Authority (*CA*), or generate your own one as explained here: :ref:`features-security-selfsigned`.

* Start the HTTPS web server, using the SSL certificate:

  .. code-block:: console

     http-server -p 8443 --ssl --cert cert.pem --key key.pem



.. _features-security-kms-wss:

Securing Kurento Media Server
=============================

With the default configuration, Kurento Media Server will use the ``ws://`` URI scheme for non-secure WebSocket connections, listening on the port ``8888``. Application Servers (Kurento clients) will establish a WebSocket connection with KMS, in order to control the media server and send messages conforming to the :doc:`/features/kurento_api`.

This is fine for initial stages of application development, but before deploying on production environments you'll probably want to move to ``wss://`` connections, i.e. using Secure WebSocket, which by default uses the port ``8433``.

To enable Secure WebSocket, edit the main KMS configuration file (*/etc/kurento/kurento.conf.json*), and un-comment the following lines:

.. code-block:: json-object

   "secure": {
     "port": 8433,
     "certificate": "cert+key.pem",
     "password": "KEY_PASSWORD"
   }

If you use a signed certificate issued by a trusted Certification Authority (*CA*) such as Verisign or Let's Encrypt, then you are done. Just skip to the next section: :ref:`features-security-kms-wss-connect`.

However, if you are going to use an untrusted self-signed certificate (typically during development), there is still more work to do.

Generate your own certificate as explained here: :ref:`features-security-selfsigned`. Now, because self-signed certificates are untrusted by nature, client browsers and server applications will reject it by default. You'll need to force all consumers of the certificate to accept it:

* **Java applications**. Follow the instructions of this link: `SunCertPathBuilderException: unable to find valid certification path to requested target <https://mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`__ (`archive <https://web.archive.org/web/20200101052022/https://mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`__).

  Get ``InstallCert.java`` from here: https://github.com/escline/InstallCert.

  You'll need to instruct the *KurentoClient* to allow using certificates. For this purpose, create an ``JsonRpcClient``:

  .. code-block:: java

     SslContextFactory sec = new SslContextFactory(true);
     sec.setValidateCerts(false);
     JsonRpcClientWebSocket rpcClient = new JsonRpcClientWebSocket(uri, sec);
     KurentoClient kurentoClient = KurentoClient.createFromJsonRpcClient(rpcClient);

* **Node applications**. Take a look at this page: `Painless Self Signed Certificates in node.js <https://git.coolaj86.com/coolaj86/ssl-root-cas.js/src/branch/master/Painless-Self-Signed-Certificates-in-node.js.md>`__ (`archive <https://web.archive.org/web/20200610093038/https://git.coolaj86.com/coolaj86/ssl-root-cas.js/src/branch/master/Painless-Self-Signed-Certificates-in-node.js.md>`__).

  For a faster but *INSECURE* alternative, configure Node to accept (instead of reject) invalid TLS certificates by default, setting the environment variable flag `NODE_TLS_REJECT_UNAUTHORIZED <https://nodejs.org/api/cli.html#cli_node_tls_reject_unauthorized_value>`__ to ``0``; this will disable the TLS validation for your whole Node app. You can set this environment variable before executing your app, or directly in your app code by adding the following line before performing the connection:

  .. code-block:: js

     process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = 0;

* **Browser JavaScript**. Similar to what happens with self-signed certificates used for HTTPS, browsers also require the user to accept a security warning before Secure WebSocket connections can be established. This is done by *directly opening* the KMS WebSocket URL: ``https://{KMS_HOST}:8433/kurento``.



.. _features-security-kms-wss-connect:

Connecting to a secured KMS
---------------------------

Now that KMS is listening for Secure WebSocket connections, and (if using a self-signed certificate) your Application Server is configured to accept the certificate used in KMS, you have to change the WebSocket URL used in your application logic.

Make sure your application uses a WebSocket URL that starts with ``wss://`` instead of ``ws://``. Depending on the platform, this is done in different ways:

* **Java**: Launch with a ``kms.url`` property. For example:

  .. code-block:: java

     mvn -U clean spring-boot:run \
         -Dspring-boot.run.jvmArguments="-Dkms.url=wss://{KMS_HOST}:8433/kurento"

* **Node**: Launch with the ``ws_uri`` command-line argument. For example:

  .. code-block:: js

     npm start -- --ws_uri="wss://{KMS_HOST}:8433/kurento"

* **Browser JavaScript**: Application-specific method. For example, using hardcoded values:

  .. code-block:: js

     const ws_uri: "wss://" + location.hostname + ":8433/kurento";



.. _features-security-selfsigned:

Generating a self-signed certificate
====================================

You need to provide a valid SSL certificate in order to enable all sorts of security features, ranging from HTTPS to Secure WebSocket (``wss://``). For this, there are two alternatives:

* Obtain a certificate from a trusted Certification Authority (*CA*). This should be your primary choice, and will be necessary for production-grade deployments.

* Create your own untrusted self-signed certificate. This can ease operations during the phase of software development. You can search articles online that explain how to do this, for example `this one <https://www.akadia.com/services/ssh_test_certificate.html>`__.

  Alternatively, it is much easier and convenient to use a self-signed certificate generation tool, such as `mkcert <https://github.com/FiloSottile/mkcert>`__. This kind of tools already take into account the requisites and limitations of most popular applications and browsers, so that you don't need to.

  Note that while a self-signed certificate can be used for web development, browsers will show a big security warning. Users will see this warning, and must click to accept the unsafe certificate before proceeding to the page.

  To generate certificates with *mkcert*, run these commands:

  .. code-block:: console

     CAROOT="$PWD" mkcert -cert-file ./cert.pem -key-file ./key.pem \
         "127.0.0.1" \
         "::1"       \
         "localhost" \
         "a.test"    \
         "b.test"    \
         "c.test"

     chmod 440 *.pem
