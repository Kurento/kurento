==============
Kurento Client
==============

Currently, the Kurento project provides implementations of the :doc:`/features/kurento_protocol` for two programming languages: *Java* and *JavaScript*.

In the future, additional Kurento Clients can be created, exposing the same kind of modularity in other languages such as Python, C/C++, PHP, etc.



Kurento Java Client
===================

**Kurento Java Client** is a Java SE layer which consumes the Kurento API and exposes its capabilities through a simple-to-use interface based on Java POJOs representing Media Elements and Media Pipelines.

This API is abstract in the sense that all the non-intuitive inherent complexities of the internal Kurento Protocol workings are abstracted and developers do not need to deal with them when creating applications. Using the Kurento Java Client only requires adding the appropriate dependency to a *Maven* project or to download the corresponding *jar* into the application's *Java Classpath*.

It is important to remark that the Kurento Java Client is a media-plane control API. In other words, its objective is to expose the capability of managing media objects, but it does not provide any signaling plane capabilities.



Kurento JavaScript Client
=========================

**Kurento JavaScript Client** is a JavaScript layer which consumes the Kurento API and exposes its capabilities to JavaScript developers. It allow to build *Node.js* and browser based applications.



Reference Documentation
=======================

- `Kurento Client JavaDoc <../_static/client-javadoc/index.html>`__
- `Kurento Client JsDoc <../_static/client-jsdoc/index.html>`__
- `Kurento Js Utils <../_static/utils-jsdoc/index.html>`__: a JavaScript utility library aimed to simplify the development of WebRTC applications.
