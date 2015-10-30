%%%%%%%%%%%%%%%%%%%%%%%%%%
Repository internals
%%%%%%%%%%%%%%%%%%%%%%%%%%

The internal library of Kurento Repository provides an API that can
be used to manage media repositories based on filesystem or :term:`MongoDB`.

The chosen transport for the media is the HTTP protocol. The repository 
API will provide managed URIs which the application or 
:term:`Kurento Media Server` can use to upload or download media. 

This library can be configured and instantiated as a Spring bean. Although, 
it shouldn't be used directly but through the :doc:`repository server <server>` 
which offers a REST and Java APIs which should suffice for most applications.
