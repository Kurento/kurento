[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento Utils for Node.js and Browsers
======================================

The Kurento Utils project contains a set of reusable components that have been
found useful during the development of the WebRTC applications with Kurento.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] and [Bower] in your system:

```bash
sudo add-apt-repository ppa:chris-lea/node.js
sudo apt-get update
sudo apt-get install nodejs
sudo npm install -g bower
```

To install the library, it's recommended to do that from the [NPM repository] :

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

[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kurento-utils
[grunt]: http://gruntjs.com/
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js]: http://nodejs.org/
[NPM repository]: https://www.npmjs.org/package/kurento-utils
[website]: http://kurento.org
