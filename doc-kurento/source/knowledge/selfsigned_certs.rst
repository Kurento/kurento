========================
Self-Signed Certificates
========================

.. contents:: Table of Contents

You need to provide a valid SSL certificate in order to enable all sorts of security features, ranging from HTTPS to Secure WebSocket (``wss://``). For this, there are two alternatives:

* Obtain a **trusted certificate** signed by a Certification Authority (*CA*). This should be your primary choice for final production deployments of the software.

* Make a custom, **untrusted self-signed certificate**. This can ease operations during the phase of software development and make testing easier.

  Web browsers show a big security warning that must be accepted by the user. Other non-browser applications will also need to be configured to bypass security checks. This should not be a problem, given that it will only happen during development and testing.

  .. warning::

     **iOS Safari** is the big exception to the above comment. It will outright reject untrusted self-signed certs, instead of showing a security warning.

     To test your app with iOS Safari and a self-signed cert, the cert's Root CA needs to be installed in the device itself: :ref:`knowledge-selfsigned-trust`.

We strongly recommend **using a certificate generation tool** such as `mkcert <https://github.com/FiloSottile/mkcert>`__. While it is perfectly fine to issue OpenSSL commands directly, the web is full of *outdated* tutorials and you'll probably end up running into lots of pitfalls, such as newer strict browser policies, or technicalities like which optional fields should be used. A tool such as *mkcert* already takes into account all these aspects, so that you don't need to.

To generate new certificate files with *mkcert*, first install the program:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --yes \
       ca-certificates libnss3-tools wget

   sudo wget -O /usr/local/bin/mkcert 'https://github.com/FiloSottile/mkcert/releases/download/v1.4.1/mkcert-v1.4.1-linux-amd64'
   sudo chmod +x /usr/local/bin/mkcert

Then run it:

.. code-block:: shell

   # Generate new untrusted self-signed certificate files.
   CAROOT="$PWD" mkcert -cert-file cert.pem -key-file key.pem \
       "127.0.0.1"   \
       "::1"         \
       "localhost"   \
       "*.home.arpa" \
       "*.home.local"

   # Set correct permissions.
   chmod 440 *.pem

This command already includes some useful things:

* It will create a new Root CA if none existed already.
* It will use the Root CA to spawn a new server certificate from it.
* The new certificate allows accessing the server from localhost in its IPv4, IPv6, and hostname forms.
* The cert also allows accessing the server if it gets assigned a name in the ``.home.arpa`` and ``.home.local`` subdomains, so other devices in your internal LAN can be used as test clients. Check the section below for examples of how to easily assign a domain name to your server.

.. note::

   * It is not possible to create certificates for a range of IP addresses (e.g. "192.168.1.\*"). You could add to the command the IP address of your server, however that solution lacks flexibility and the cert won't work if (when) the server's IP is different. That's why private domain names end up being a better solution for LAN testing.

   * ``.home.arpa`` is the IETF recommended subdomain for use in private networks. You can check more info in `What domain name to use for your home network <https://www.ctrl.blog/entry/homenet-domain-name.html>`__ and :rfc:`8375`.

   * ``.local`` is the Zeroconf subdomain for mDNS protocol. It will be useful if using Zeroconf to assign a domain name to the server, as explained below. Many clients (i.e. web browsers) don't support second-level wildcards like "\*.local", so that's why we propose "\*.home.local" here.



Using a local domain
====================

With the Hosts file
-------------------

You can take advantage of a subdomain like ``.home.arpa`` by adding a new entry to the *Hosts file* in client devices that will connect to your test server.

This is an easy thing to do, but has the drawback of having to change each client separately. Also, doing this is easy on desktop computers, but not so easy (or outright impossible) on mobile devices.

For Linux and macOS you just need to add a line like this to your ``/etc/hosts`` file (but with the correct IP address of your server):

.. code-block:: text

   192.168.1.50  server.home.arpa

Now, opening ``server.home.arpa`` on that client will access your test server located at 192.168.1.50.

On Windows you can do the same; the Hosts file is located at ``%SystemRoot%\System32\drivers\etc\hosts``.

Different systems have this file in different locations, so check here for a more complete list: :wikipedia:`Hosts_(file)#Location_in_the_file_system`.



With Zeroconf (mDNS)
--------------------

You can publish your server IP address as a **discoverable Zeroconf name** in your LAN. This is a more flexible solution than editing Hosts files in every client device, as it only needs to be done once, in the server itself.

An even more general solution than this would be to use a full-fledged DNS server, but using Zeroconf is a simpler solution that can be set up quickly by any developer.

For example, if your test server uses Ubuntu, ensure the *avahi-publish* tool is installed:

.. code-block:: shell

   sudo apt-get update && sudo apt-get install avahi-utils

And run this:

.. code-block:: shell

   # Get the IP address to the default network gateway.
   IP_ADDRESS="$(ip -4 -oneline route get 1.0.0.0 | grep -Po 'src \K([\d.]+)')"

   # Publish the IP address as a Zeroconf name.
   avahi-publish --address --no-reverse "server.home.local" "$IP_ADDRESS"

This technique is very handy, because all popular modern platforms include mDNS clients to discover Zeroconf addresses:

* Windows, since Windows 10: `mDNS and DNS-SD slowly making their way into Windows 10 <https://www.ctrl.blog/entry/windows-mdns-dnssd.html>`__.
* Mac and iOS include mDNS natively.
* Linux systems support mDNS if the appropriate `Avahi <https://www.avahi.org/>`__ packages are installed.
* Android supports mDNS resolution since API Level 32 aka. Android 12.1: `mDNS .local resolution <https://source.android.com/docs/core/ota/modular-system/dns-resolver#mdns-local-resolution>`__. Android 12.0 might also have the feature backported on some devices, according to user comments in the `feature issue <https://issuetracker.google.com/issues/140786115>`__.



.. _knowledge-selfsigned-trust:

Trusting a self-signed certificate
==================================

Most clients won't trust a self-signed certificate when connecting to a server that uses one. What the client will do is to block the connection with an error message (this is what iOS Safari does, also Node.js apps); or show a security warning page (like with Chrome and Firefox web browsers).

Normally, there is some way to override this behavior. Either by installing your Root CA in the device's or client's special cert storage, or by setting some configuration. Then, the self-signed certificate will be trusted just like if it had been issued by a reputable Authority.



On desktop browsers
-------------------

Installing the Root CA is easy because *mkcert* does it for you. In the terminal, go to the dir where your ``rootCA.pem`` file is located, and run:

.. code-block:: shell

   CAROOT="$PWD" mkcert -install



On mobile devices
-----------------

Installing the Root CA is a bit more difficult:

* With iOS, you can either email the ``rootCA.pem`` file to yourself, use AirDrop, or serve it from an HTTP server. Normally, a dialog should pop up asking if you want to install the new certificate; afterwards, you must `enable full trust in it <https://support.apple.com/en-us/HT204477>`__. When finished, your self-signed certs will be trusted by the system, and iOS Safari will allow accessing pages on the ``.home.arpa`` subdomain.

  .. note::

     Only AirDrop, Apple Mail, or Safari are allowed to download and install certificates on iOS. Other applications will not work for this.

* With Android, you'll have to install the Root CA and then enable user roots in the development build of your app. See `this StackOverflow answer <https://stackoverflow.com/a/22040887/749014>`__.



On Node.js applications
-----------------------

Node.js does not use the system root store, so it won't accept mkcert certificates automatically. Instead, you will have to set the [`NODE_EXTRA_CA_CERTS`](https://nodejs.org/api/cli.html#cli_node_extra_ca_certs_file) environment variable:

.. code-block:: shell

   export NODE_EXTRA_CA_CERTS="/path/to/rootCA.pem"

You could add such env var on every launch, on the project's ``package.json`` file, or on the system's ``~/.profile`` file, so it will get automatically set for you.
