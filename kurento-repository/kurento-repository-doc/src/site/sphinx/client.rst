%%%%%%%%%%%%%%%%%
Repository client
%%%%%%%%%%%%%%%%%

This Java library belonging to the Kurento Repository stack has been designed 
using the :term:`Retrofit` framework in order to provide a Java wrapper for
the :ref:`HTTP REST interface <server-rest-api>` exposed by the repository server. 

Repository Java API
-------------------

This API maps directly over the :ref:`REST interface layer<server-rest-api>`, in 
such a way that the primitives exposed by this library mirror the REST ones.

The Java documentation included in ``org.kurento.repository.RepositoryClient`` and
its factory ``org.kurento.repository.RepositoryClientProvider`` is quite 
detailed so it shouldn't be very difficult to set up a client connected to a 
running Kurento Repository Server (don't forget to check our tutorials, and 
especially the 
`kurento-hello-world-repository <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-hello-world-recording>`_
one).

Usage
-----

This library can be imported as a Maven dependency and then instances of 
``org.kurento.repository.RepositoryClient`` can be created in order to interact 
with the repository server.

We provide a Kurento Java tutorial, 
`kurento-hello-world-repository <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-hello-world-recording>`_
, which uses this library to save the streamed media from a web browser using a 
repository server instance.