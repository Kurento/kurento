%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Content API Documentation
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Introduction
============

The Kurento Content API (`javadoc <../_static/index.html?com/kurento/kmf/content/package-summary.html>`__)
is a Java EE layer which consumes the :doc:`Kurento Media API  </kmf-media-api/packages>` and exposes its capabilities
through a simple modularity based on two types of objects: :java:type:`ContentHandlers <ContentHandler>`
and :java:type:`ContentSessions <ContentSession>`.

A :java:type:`ContentHandler` is an abstraction extending the Java EE Servlet API. It enables
the development of multimedia applications by managing the reaction to signaling events 
that happen during a session (e.g. :java:meth:`onContentRequest`,
:java:meth:`onContentTerminated`, etc.).

`ContentSessions` represent specific client applications accessing to the infrastructure
and have an associated state. The Content API is a signaling plane API, which makes possible
to react to signaling messages received from the client and to execute the appropriate
application logic (e.g. authenticate, connect to a database, execute a web service, use
the :doc:`Media API </kmf-media-api/packages>`, etc.) at the appropriate instants.
Content API developers require a Java EE compatible Application Server.

API Javadoc
===========

This is the standard `javadoc of the Kurento Content API <../_static/index.html?com/kurento/kmf/content/package-summary.html>`__ classes.

API Classes
===========

.. toctree::
   :maxdepth: 2

   com/kurento/kmf/content/package-index

