[![][KurentoImage]][website]

Copyright © 2013 Kurento. Licensed under [LGPL License].

KWS Media API
=============
Media API for Kurento Web SDK

The KWS Media API project contains the implementation of the Kurento client
side Media API for web applications.

The source code of this project can be cloned from the [GitHub repository].

How to test
-----------
Tests are autonomous, only requirement is to have exec ```npm install``` to have
installed all the dev dependencies.

To exec tests in browser, you only need to open the file ```test/index.html```
and it will launch automatically using [QUnit]. You can be able to configure to
what WebSocket endpoint you want to connect on the dropdown at the top of the
tests page.

To exec test in Node.js, you only need to exec ```npm test``` that will launch
all the tests automatically using [QUnit-cli].

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
