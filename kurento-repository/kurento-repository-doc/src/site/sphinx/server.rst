%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Repository server application
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

The server module of Kurento Repository is a :term:`Spring Boot` 
application which exposes the configured media repository through an 
easy-to-use :term:`HTTP` :term:`REST` API.

This module can also be integrated into existing applications instead
of a standalone execution. The application will have access to a Java
API which wraps around the repository internal library.

There is a Kurento Java 
`tutorial application <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-hello-world-recording>`_ 
that employs a running instance of this server to record and play media over HTTP 
using the capabilities of the `Kurento Media Server <http://www.kurento.org>`_.

In order to learn how to build, configure and run the server application, please
refer to the :doc:`next section <run_server>`.

The APIs that can be used to communicate with the server are described in the 
server API :doc:`section <apis_server>`.