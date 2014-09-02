.. _kmf-content-api:

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Content API Documentation
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Introduction
============

The Kurento Content API
(`javadoc <../javadoc/index.html?com/kurento/kmf/content/package-summary.html>`__)
is a Java EE layer which consumes the
:doc:`Kurento Media API </kmf-media-api/packages>` and exposes its capabilities
through a simple modularity based on two types of objects: `ContentHandlers`
and `ContentSessions`.

A :java:type:`ContentHandler` is an abstraction extending the :term:`Java EE`
Servlet API. It enables the development of :term:`multimedia` applications by
managing the reaction to signalling events that happen during a session (e.g.
:java:meth:`onContentRequest`, :java:meth:`onContentTerminated`, etc.).

A :java:type:`ContentSession` represents a specific client application accessing
to the infrastructure and has an associated state. The Content API is a
:term:`signalling plane` API, which makes possible to react to signalling
messages received from the client and to execute the appropriate application
logic (e.g. authenticate, connect to a database, execute a web service, use the
:doc:`Media API  </kmf-media-api/packages>`, etc.) at the appropriate instants.
Content API developers require a Java EE compatible Application Server.

API Javadoc
===========

This is the standard
`javadoc of the Kurento Content API <../javadoc/index.html?com/kurento/kmf/content/package-summary.html>`__
classes.

API Classes
===========

.. java:package:: com.kurento.kmf.content

.. inheritance-diagram:: sphinx.ext.inheritance_diagram

.. toctree::
   :maxdepth: 2

   com/kurento/kmf/content/package-index

