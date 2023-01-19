=============================
Securing Kurento Applications
=============================

.. contents:: Table of Contents



Securing Application Servers
============================

.. _features-security-java-https:

Configure a Java server to use HTTPS
------------------------------------

* Obtain a certificate. For this, either request one from a trusted Certification Authority (*CA*), or generate your own one as explained here: :doc:`/knowledge/selfsigned_certs`.

* Convert your PEM certificate to either `Java KeyStore <https://en.wikipedia.org/wiki/Java_KeyStore>`__ (*JKS*) or `PKCS#12 <https://en.wikipedia.org/wiki/PKCS_12>`__. The former is a proprietary format limited to the Java ecosystem, while the latter is an industry-wide used format. To make a PKCS#12 file from an already existing PEM certificate, run these commands:

  .. code-block:: shell

     openssl pkcs12 \
         -export \
         -in cert.pem -inkey key.pem \
         -out cert.p12 -passout pass:123456

     chmod 440 *.p12

* Use the certificate in your application.

  Place your PKCS#12 file *cert.p12* in ``src/main/resources/``, and add this to the *application.properties* file:

  .. code-block:: properties

     server.ssl.key-store=classpath:cert.p12
     server.ssl.key-store-password=123456
     server.ssl.key-store-type=PKCS12

* Start the Spring Boot application:

  .. code-block:: shell

     mvn spring-boot:run \
         -Dspring-boot.run.jvmArguments="-Dkms.url=ws://{KMS_HOST}:8888/kurento"

.. note::

   If you plan on using a webserver as proxy, like Nginx or Apache, you'll need to *setAllowedOrigins* when registering the handler. Please read the `official Spring documentation <https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-server-allowed-origins>`__ entry for more info.



.. _features-security-node-https:

Configure a Node.js server to use HTTPS
---------------------------------------

* Obtain a certificate. For this, either request one from a trusted Certification Authority (*CA*), or generate your own one as explained here: :doc:`/knowledge/selfsigned_certs`.

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

.. code-block:: shell

   curl -sSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
   sudo apt-get install nodejs
   sudo npm install -g http-server

* Obtain a certificate. For this, either request one from a trusted Certification Authority (*CA*), or generate your own one as explained here: :doc:`/knowledge/selfsigned_certs`.

* Start the HTTPS web server, using the SSL certificate:

  .. code-block:: shell

     http-server -p 8443 --ssl --cert cert.pem --key key.pem



Securing Kurento Media Server
=============================

Signaling Plane authorization
-----------------------------

You should protect the JSON-RPC API control port (WebSocket port, by default TCP 8888) of your Kurento Media Server instances from unauthorized access from public networks.

The Kurento WebSocket server supports using SSL certificates in order to guarantee secure communications between clients and server; however, at the time no authentication mechanism is provided. Kurento doesn't reinvent the wheel here including its own mechanism, and instead it relies on layers of security that already exist at the system level. This is something we may add (contributions are welcomed!) but for now here are some tips on how other big players are protecting KMS from unauthorized use.

Think of KMS like you would of a database in a traditional web application; there are two levels:

1. The **application level**. We usually call this the ":doc:`Application Server </user/writing_applications>`" of Kurento Media Server. It usually is a web application that uses :doc:`/features/kurento_client` to access features of :doc:`/features/kurento_modules`.
2. The **media level** (actual audio/video transmissions to/from KMS).

The idea is that nobody unauthorized should be able to access the exchanged media. At the application level we can use all the available techniques used to protect any web server, for example with a custom user/password mechanism. Regarding KMS, the idea is that only the *Application Server* can access KMS. We can restrict that at the system level, for example using `iptables <https://linux.die.net/man/8/iptables>`__ to restrict all incoming WebSocket connections to KMS only from a given host, or a given subnet, similar to this: `Iptables Essentials: Common Firewall Rules and Commands <https://www.digitalocean.com/community/tutorials/iptables-essentials-common-firewall-rules-and-commands>`__ (`archive <https://archive.is/frjCa>`__). It may be a good idea to have the *Application Server* running in the same host than the Media Server, and in that case just restrict incoming connections to the same host.

If you need more flexibility, one idea is to restrict KMS connections to the same host using iptables and then implement a WebSocket proxy in the same machine (e.g. using nginx) that has its resources secured, as in `NGINX as a WebSocket Proxy <https://www.nginx.com/blog/websocket-nginx/>`__ (`archive <https://archive.is/xqbUJ>`__) or `WebSocket proxying <https://nginx.org/en/docs/http/websocket.html>`__ (`archive <https://archive.is/ZvqCG>`__); this way, the *Application Server* connects to the WebSocket proxy that can indeed be secured, and thus only authenticated users from remote hosts can gain access to KMS.



.. _features-security-kms-wss:

Signaling Plane security (WebSocket)
------------------------------------

With the default configuration, Kurento Media Server will use the ``ws://`` URI scheme for non-secure WebSocket connections, listening on the port TCP 8888. Application Servers (Kurento clients) will establish a WebSocket connection with KMS, in order to control the media server and send messages conforming to the :doc:`/features/kurento_protocol`.

This is fine for initial stages of application development, but before deploying on production environments you'll probably want to move to ``wss://`` connections, i.e. using Secure WebSocket, which by default uses the port TCP 8433.

To enable Secure WebSocket, edit the main KMS configuration file (``/etc/kurento/kurento.conf.json``), and un-comment the following lines:

.. code-block:: json

   "secure": {
     "port": 8433,
     "certificate": "cert+key.pem",
     "password": "KEY_PASSWORD"
   }

If you use a signed certificate issued by a trusted Certification Authority (*CA*) such as Verisign or Let's Encrypt, then you are done. Just skip to the next section: :ref:`features-security-kms-wss-connect`.

However, if you are going to use an untrusted self-signed certificate (typically during development), there is still more work to do.

Generate your own certificate as explained here: :doc:`/knowledge/selfsigned_certs`. Now, because self-signed certificates are untrusted by nature, client browsers and server applications will reject it by default. You'll need to force all consumers of the certificate to accept it:

* **Java applications**. Follow the instructions of this link: `SunCertPathBuilderException: unable to find valid certification path to requested target <https://mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`__ (`archive <https://web.archive.org/web/20200101052022/https://mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`__).

  Get *InstallCert.java* from here: https://github.com/escline/InstallCert.

  You'll need to instruct the *KurentoClient* to allow using certificates. For this purpose, create an *JsonRpcClient*:

  .. code-block:: java

     SslContextFactory sec = new SslContextFactory(true);
     sec.setValidateCerts(false);
     JsonRpcClientWebSocket rpcClient = new JsonRpcClientWebSocket(uri, sec);
     KurentoClient kurentoClient = KurentoClient.createFromJsonRpcClient(rpcClient);

* **Node.js applications**. Take a look at this page: `Painless Self Signed Certificates in node.js <https://git.coolaj86.com/coolaj86/ssl-root-cas.js/src/branch/master/Painless-Self-Signed-Certificates-in-node.js.md>`__ (`archive <https://web.archive.org/web/20200610093038/https://git.coolaj86.com/coolaj86/ssl-root-cas.js/src/branch/master/Painless-Self-Signed-Certificates-in-node.js.md>`__).

  For a faster but *INSECURE* alternative, configure Node.js to accept (instead of reject) invalid TLS certificates by default, setting the environment variable flag `NODE_TLS_REJECT_UNAUTHORIZED <https://nodejs.org/api/cli.html#cli_node_tls_reject_unauthorized_value>`__ to *0*; this will disable the TLS validation for your whole Node.js app. You can set this environment variable before executing your app, or directly in your app code by adding the following line before performing the connection:

  .. code-block:: js

     process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = 0;

* **Browser JavaScript**. Similar to what happens with self-signed certificates used for HTTPS, browsers also require the user to accept a security warning before Secure WebSocket connections can be established. This is done by *directly opening* the KMS WebSocket URL: ``https://{KMS_HOST}:8433/kurento``.



.. _features-security-kms-wss-connect:

Connecting to Secure WebSocket
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now that KMS is listening for Secure WebSocket connections, and (if using a self-signed certificate) your Application Server is configured to accept the certificate used in KMS, you have to change the WebSocket URL used in your application logic.

Make sure your application uses a WebSocket URL that starts with ``wss://`` instead of ``ws://``. Depending on the platform, this is done in different ways:

* **Java**: Launch with a *kms.url* property. For example:

  .. code-block:: shell

     mvn spring-boot:run \
         -Dspring-boot.run.jvmArguments="-Dkms.url=wss://{KMS_HOST}:8433/kurento"

* **Node.js**: Launch with the *ws_uri* command-line argument. For example:

  .. code-block:: js

     npm start -- --ws_uri="wss://{KMS_HOST}:8433/kurento"

* **Browser JavaScript**: Application-specific method. For example, using hardcoded values:

  .. code-block:: js

     const ws_uri: "wss://" + location.hostname + ":8433/kurento";



.. _features-security-kms-dtls:

Media Plane security (DTLS)
---------------------------

WebRTC uses :wikipedia:`DTLS <Datagram_Transport_Layer_Security>` for media data authentication. By default, if no certificate is provided for this, Kurento Media Server will auto-generate a new self-signed certificate for every WebRtcEndpoint instance.

Alternatively, an already existing certificate can be provided to be used for all endpoints. For this, edit the file ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini`` and set either *pemCertificateRSA* or *pemCertificateECDSA* with a file containing both your certificate (chain) file(s) and the private key. You can generate such file with the ``cat`` command:

.. code-block:: shell

   # Make a single file to be used with Kurento Media Server.
   cat cert.pem key.pem >cert+key.pem

Then, :ref:`configure <configuration-dtls>` the path to ``cert+key.pem``.

Setting a custom certificate for DTLS is needed, for example, for situations where you have to manage multiple media servers and want to make sure that all of them use the same certificate for their connections. Some browsers, such as Firefox, require this in order to allow multiple WebRTC connections from the same tab to different KMS instances.
