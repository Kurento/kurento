========================
Self-Signed Certificates
========================

.. contents:: Table of Contents

You need to provide a valid SSL certificate in order to enable all sorts of security features, ranging from HTTPS to Secure WebSocket (``wss://``). For this, there are two alternatives:

* Obtain a **trusted certificate** signed by a Certification Authority (*CA*). This should be your primary choice for final production deployments of the software.

* Make a custom, **untrusted self-signed certificate**. This can ease operations during the phase of software development and make testing easier.

  A self-signed certificate will make browsers show a big security warning that must be accepted by the user. Other non-browser applications will also need to be configured to bypass security checks. This should not be a problem, given that it will only happen during development and testing.

  .. warning::

     **iOS Safari** is the big exception to the above comment. It will outright reject untrusted self-signed certs, instead of showing a security warning.

     To test your app with iOS Safari and a self-signed cert, the cert root needs to be installed in the device itself: :ref:`knowledge-selfsigned-trust`.

There are lots of articles that explain how to make a self-signed certificate, such as `this one <https://www.akadia.com/services/ssh_test_certificate.html>`__. Instead, **we recommend using a certificate generation tool** such as `mkcert <https://github.com/FiloSottile/mkcert>`__. It is perfectly fine to use OpenSSL commands directly, but the web is full of outdated tutorials and you'll probably end up running into lots of pitfalls due to frequent updates on browser policies that dictate how certificates should be generated. A cert generation tool already takes into account the requisites and limitations of most popular applications and browsers, so that you don't need to.

To generate new certificate files with mkcert, run these commands:

.. code-block:: shell

   # Generate new untrusted self-signed certificate files.
   CAROOT="$PWD" mkcert -cert-file cert.pem -key-file key.pem \
       "127.0.0.1" \
       "::1"       \
       "localhost" \
       "*.test.local"

   # Make a single file to be used with Kurento Media Server.
   cat cert.pem key.pem > cert+key.pem

   # Protect against writes.
   chmod 440 *.pem

This command already includes some useful things:

* Allows accesses from localhost in its IPv4, IPv6, and hostname forms.
* The ``*.test.local`` **domain wildcard**, meant to make the development machine accessible through any desired subdomain(s). This way, the cert files can be used not only for localhost, but also for testing in your LAN.

.. note::

   Simply using ``*.local`` would be nice, but wildcards are forbidden for global TLDs, so it wouldn't work. For example, MacOS 10.15 (*Catalina*) actively rejects such certificates (see `mkcert bug 206 <https://github.com/FiloSottile/mkcert/issues/206>`__). For this reason, we propose using ``*.test.local``.



Using a local domain
====================

With the Hosts file
-------------------

You can take advantage of a domain wildcard such as ``*.test.local``, by adding a new entry to the *Hosts file* in client computers that will connect to your main development machine.

For example, on Linux and macOS you could add this line to your ``/etc/hosts`` file:

.. code-block:: text

   192.168.1.50  dev.test.local

After editing the Hosts file like in this example, you can open a Firefox or Chrome browser, put ``dev.test.local`` in the address bar, and access your main development machine at 192.168.1.50.

On Windows you can do the same; the Hosts file is located at ``%SystemRoot%\System32\drivers\etc\hosts``. Different systems have this file in different locations, so check here for a more complete list: :wikipedia:`Hosts_(file)#Location_in_the_file_system`.



With Zeroconf
-------------

If editing the Hosts file is not an option, or you would like a more flexible solution, another possibility is to publish your server IP address as a temporary domain name in your LAN. You could do this with a full-fledged DNS server, but a simpler solution is to assign your machine a **discoverable Zeroconf address**.

This technique is very handy, because practically all modern platforms include an mDNS client to discover Zeroconf addresses. For example, if your development machine uses Ubuntu, you can run this:

.. code-block:: shell

   # Get and publish the IP address to the default network gateway.
   IP_ADDRESS="$(ip -4 -oneline route get 1.0.0.0 | grep -Po 'src \K([\d.]+)')"
   avahi-publish --address --no-reverse -v "dev.test.local" "$IP_ADDRESS"

.. note::

   As of this writing, Android seems to be the only major platform unable to resolve Zeroconf addresses. All other systems support them in one way or another:

   * Windows: `mDNS and DNS-SD slowly making their way into Windows 10 <https://www.ctrl.blog/entry/windows-mdns-dnssd.html>`__.
   * Mac and iOS include mDNS natively.
   * Linux systems support mDNS if the appropriate Avahi packages are installed.

   You can vote for adding mDNS to Android by adding a star ⭐ (top, next to the title) on this issue: `#140786115 Add .local mDNS resolving to Android <https://issuetracker.google.com/140786115>`__ (requires login; any Google account will do). **Please refrain from commenting "+1"**, which sends a useless email to all other users who follow the issue.



.. _knowledge-selfsigned-trust:

Trusting a self-signed certificate
==================================

Most browsers will not trust a self-signed certificate, showing a security warning page (or rejecting access altogether, like iOS Safari). However, you can override this by installing your Root CA. The self-signed certificate will be trusted as if it had been issued by a reputable Authority.

On desktop browsers, installing the Root CA is easy because mkcert does it for you:

.. code-block:: shell

   CAROOT="$PWD" mkcert -install

On mobile devices, installing the Root CA is a bit more difficult:

* With iOS, you can either email the ``rootCA.pem`` file to yourself, use AirDrop, or serve it from an HTTP server. Normally, a dialog should pop up asking if you want to install the new certificate; afterwards, you must `enable full trust in it <https://support.apple.com/en-nz/HT204477>`__. When finished, your self-signed certs will be trusted by the system, and iOS Safari will allow accessing pages on the ``*.test.local`` subdomain.

  .. note::

     Only AirDrop, Apple Mail, or Safari are allowed to download and install certificates on iOS. Other applications will not work for this.

* With Android, you will have to install the Root CA and then enable user roots in the development build of your app. See `this StackOverflow answer <https://stackoverflow.com/a/22040887/749014>`__.
