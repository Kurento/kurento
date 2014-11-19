[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento Utils for Bower
=======================

The Kurento Utils project contains a set of reusable components that have been
found useful during the development of the WebRTC applications with Kurento.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] and [Bower] in your system:

```bash
curl -sL https://deb.nodesource.com/setup | sudo bash -
sudo apt-get install -y nodejs
sudo npm install -g bower
```

To install the library, it's recommended to do that from the [Bower repository] :

```bash
bower install kurento-utils
```

Alternatively, you can download the code using git and install manually its
dependencies:

```bash
git clone https://github.com/Kurento/kurento-utils-bower
cd kurento-utils-bower
npm install
```

Kurento
=======

What is Kurento
---------------
Kurento provides an open platform for video processing and streaming based on
standards.

This platform has several APIs and components which provide solutions to the
requirements of multimedia content application developers. These include:

  * Kurento Media Server (KMS). A full featured media server providing
    the capability to create and manage dynamic multimedia pipelines.
  * Kurento Clients. Libraries to create applications with media
    capabilities. Kurento provides libraries for Java, browser JavaScript,
    and Node.js.

Downloads
---------
To download binary releases of Kurento components visit http://kurento.org

Code for other Kurento projects can be found in the [GitHub Kurento group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].

[Bower]: http://bower.io
[Bower repository]: https://github.com/Kurento/kurento-utils-bower
[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kurento-utils
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js]: http://nodejs.org/
[website]: http://kurento.org
