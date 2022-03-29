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

   sudo apt-get update && sudo apt-get install --yes \
       ca-certificates libnss3-tools wget

   sudo wget -O /usr/local/bin/mkcert 'https://github.com/FiloSottile/mkcert/releases/download/v1.4.1/mkcert-v1.4.1-linux-amd64'
   sudo chmod +x /usr/local/bin/mkcert

Then run it:

.. code-block:: shell

   # Generate new untrusted self-signed certificate files.
   CAROOT="$PWD" mkcert -cert-file cert.pem -key-file key.pem \
       "127.0.0.1" \
       "::1"       \
       "localhost" \
       "*.home.arpa"

   # Set correct permissions.
   chmod 440 *.pem

This command already includes some useful things:

* It will create a new Root CA if none existed already.
* It will use the Root CA to spawn a new server certificate from it.
* The new certificate allows accessing the server from localhost in its IPv4, IPv6, and hostname forms.
* The cert also allows accessing the server at the ``*.home.arpa`` *domain wildcard*, regardless of the IP address in your network, so other machines in your internal LAN can be used for testing. Check the section below for examples of how to easily assign a domain name to your server.

.. note::

   ``*.home.arpa`` is the IETF recommended domain name for use in private networks. You can check more info in `What domain name to use for your home network <https://www.ctrl.blog/entry/homenet-domain-name.html>`__ and :rfc:`8375`.



Using a local domain
====================

With the Hosts file
-------------------

You can take advantage of a domain wildcard such as ``*.home.arpa``, by adding a new entry to the *Hosts file* in client machines that will connect to your test server.

This is a quick and dirty technique, but has the drawback of having to do the change separately on each client. Also, doing this is easy in desktop computers, but not so easy (or outright impossible) on mobile devices.

On Linux and macOS, add a line like this to your ``/etc/hosts`` file (but with the correct IP address of your server):

.. code-block:: text

   192.168.1.50  dev.home.arpa

Now, opening ``dev.home.arpa`` on a client's web browser will access your test server at 192.168.1.50.

On Windows you can do the same; the Hosts file is located at ``%SystemRoot%\System32\drivers\etc\hosts``. Different systems have this file in different locations, so check here for a more complete list: :wikipedia:`Hosts_(file)#Location_in_the_file_system`.



With Zeroconf
-------------

You can publish your server IP address as a temporary domain name in your LAN. This is a more flexible solution than editing Hosts files in every client machine, as it only needs to be done once, in the server itself.

This could be done with a full-fledged DNS server, but a simpler solution is to assign your machine a **discoverable Zeroconf address**.

For example, if your test server uses Ubuntu, run this:

.. code-block:: shell

   # Get and publish the IP address to the default network gateway.
   IP_ADDRESS="$(ip -4 -oneline route get 1.0.0.0 | grep -Po 'src \K([\d.]+)')"
   avahi-publish --address --no-reverse -v "dev.home.arpa" "$IP_ADDRESS"

This technique is very handy, because practically all modern platforms include an mDNS client to discover Zeroconf addresses in the LAN.

.. note::

   As of this writing, Android seems to be the only major platform unable to resolve Zeroconf addresses. All other systems support them in one way or another:

   * Windows: `mDNS and DNS-SD slowly making their way into Windows 10 <https://www.ctrl.blog/entry/windows-mdns-dnssd.html>`__.
   * Mac and iOS include mDNS natively.
   * Linux systems support mDNS if the appropriate Avahi packages are installed.

   You can vote for adding mDNS to Android by adding a star ⭐ (top, next to the title) on this issue: `#140786115 Add .local mDNS resolving to Android <https://issuetracker.google.com/140786115>`__ (requires login; any Google account will do). **Please refrain from commenting "+1"**, which sends a useless email to all other users who follow the issue.



.. _knowledge-selfsigned-trust:

Trusting a self-signed certificate
==================================

Most browsers will not trust a self-signed certificate, showing a security warning page (or rejecting access altogether, like iOS Safari). However, you can override this by installing your Root CA in the device. Then, the self-signed certificate will be trusted just like if it had been issued by a reputable Authority.

On desktop browsers, installing the Root CA is easy because *mkcert* does it for you:

.. code-block:: shell

   CAROOT="$PWD" mkcert -install

On mobile devices, installing the Root CA is a bit more difficult:

* With iOS, you can either email the ``rootCA.pem`` file to yourself, use AirDrop, or serve it from an HTTP server. Normally, a dialog should pop up asking if you want to install the new certificate; afterwards, you must `enable full trust in it <https://support.apple.com/en-nz/HT204477>`__. When finished, your self-signed certs will be trusted by the system, and iOS Safari will allow accessing pages on the ``*.home.arpa`` subdomain.

  .. note::

     Only AirDrop, Apple Mail, or Safari are allowed to download and install certificates on iOS. Other applications will not work for this.

* With Android, you'll have to install the Root CA and then enable user roots in the development build of your app. See `this StackOverflow answer <https://stackoverflow.com/a/22040887/749014>`__.
