[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento Utils for Node.js and browsers
=============
Utilities for Kurento Web SDK

The Kurento Utils project contains a set of reusable components that have been
found useful during the development of the Kurento Web SDK, and the different 
projects that serve as demo.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed the Node.js tools in your system. It's heavily
encouraged to use the latest Node.js and NPM versions from the
[Node.js project PPA] instead of the packages available on the oficial Ubuntu
repositories, since due to the fast-moving Node.js community and environment
these last ones get easily outdated and can lead to incompatibility errors:

```bash
curl -sL https://deb.nodesource.com/setup | sudo bash -

sudo apt-get install nodejs nodejs-legacy
```

To install the library, it's recomended to do that from the [NPM repository] :

```bash
npm install kurento-utils
```

Alternatively, you can download the code using git and install manually its
dependencies:

```bash
git clone https://github.com/Kurento/kurento-utils
cd kurento-utils
npm install
```

### Browser

To build the browser version of the library you'll only need to exec the [grunt]
task runner and they will be generated on the ```dist``` folder. Alternatively,
if you don't have it globally installed, you can run a local copy by executing

```bash
node_modules/.bin/grunt
```


Kurento
=======

What is Kurento
---------------
Kurento provides an open platform for video processing and streaming based on
standards.

This platform has several APIs and components which provide solutions to the
requirements of multimedia content application developers. These include:

* Kurento Media Server (KMS). A full featured media server providing the
capability to create and manage dynamic multimedia pipelines.
* Kurento Media Framework (KMF). A Java server-side API providing the required
abstractions for creating applications that manage multimedia content, calls
and conferences involving audio, video and data information.
* Kurento Web SDK (KWS). A client-side HTML5 and Javascript SDK for accessing
KMF capabilities
* Kurento Android SDK (KAS). A Java and native SDK allowing easy integration of
KMF into any kind of Android application.

Downloads
---------
To download binary releases of Kurento components visit http://kurento.org

Code for other Kurento projects can be found in the [GitHub Kurento group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].

[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kurento-utils
[grunt]: http://gruntjs.com/
[Kurento Media Connector]: https://github.com/Kurento/kmf-media-connector
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js project PPA]: https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-debian-linux-mint-etc
[NPM repository]: https://www.npmjs.org/package/kurento-utils
[website]: http://kurento.org
