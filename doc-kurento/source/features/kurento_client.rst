====================
Client API Reference
====================

Currently, the Kurento project provides implementations of the :doc:`/features/kurento_protocol` for two programming languages: *Java* and *JavaScript*.

In the future, additional Kurento Clients can be created, exposing the same kind of modularity in other languages such as Python, C/C++, PHP, etc.



Java Client
===========

**Kurento Java Client** is a Java SE layer which consumes the Kurento API and exposes its capabilities through a simple-to-use interface based on Java POJOs representing Media Elements and Media Pipelines. Using the Kurento Java Client only requires adding the appropriate dependency to a *Maven* project or to download the corresponding *jar* into the application's *Java Classpath*.

* **Reference**: `Kurento Client JavaDoc <../_static/client-javadoc/index.html>`__.



JavaScript Client
=================

**Kurento JavaScript Client** is a JavaScript layer which consumes the Kurento API and exposes its capabilities to JavaScript developers. It allow to build *Node.js* and browser-based applications.

* **Reference**: `Kurento Client JsDoc <../_static/client-jsdoc/index.html>`__.



Kurento Js Utils
================

**kurento-utils-js** (``browser/kurento-utils-js/``) is a browser library that can be used to simplify creation and handling of `RTCPeerConnection <https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection>`__ objects, to control the browser's `WebRTC API <https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API>`__.

.. warning::

   This library is not actively maintained. It was written to simplify the :doc:`Kurento Tutorials </user/tutorials>` and has several shortcomings for more advanced uses.

   For real-world applications we recommend to **avoid using this library**  and instead to write your JavaScript code directly against the browser's WebRTC API.

* **Reference**: `kurento-utils-js JsDoc <../_static/utils-jsdoc/index.html>`__.
