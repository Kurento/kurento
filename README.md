[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

KWS Media API
=============
Media API for Kurento Web SDK

The KWS Media API project contains the implementation of the Kurento client
side Media API for web applications.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure Install node tools in your system.

In ubuntu:

```bash
sudo apt-get install nodejs-legacy
sudo apt-get install npm
```

After that, to build the browser version of the API you'll only need to exec
```node_modules/.bin/grunt``` and they will be generated on the
```dist``` folder.


How to test
-----------
Tests are autonomous, only requirement is to exec previously ```npm install```
to have installed all the dev dependencies.

### Browser

To exec tests in browser, you need to build the browser version of the library
with ```node_modules/.bin/grunt```.

After that, just open the file ```test/index.html``` with any browser, and the
tests will launch automatically using [QUnit]. In case of the browser raising
security policy errors, you can host the code using any static web server from
the source code root folder, for example using the command
```python -m SimpleHTTPServer 8000```.

You can be able to configure to what WebSocket endpoint you want to connect on
the dropdown at the top of the tests page.

### Node.js

To exec test in Node.js, you only need to exec ```npm test``` that will launch
all the tests automatically using [QUnit-cli] (At the moment, the default IP can
not be changed.

If you need to use a WebSocket endpoint different from the default one, you can exec the underlying test command with
```node_modules/.bin/qunit-cli -c KwsMedia:. -c wock:node_modules/wock -c test/_common.js -c test/_proxy.js test/*.js``` and append the *ws_uri* parameter.

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

[KurentoImage]: https://0.gravatar.com/avatar/b8fffabbe3831731cb4c4c9667bfa439?s=120
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[GitHub repository]: https://github.com/kurento/kws-media-api
[GitHub Kurento group]: https://github.com/kurento
[website]: http://kurento.org
[kurentoms]: http://twitter.com/kurentoms
[QUnit]: http://qunitjs.com
[QUnit-cli]: https://github.com/devongovett/qunit-cli
