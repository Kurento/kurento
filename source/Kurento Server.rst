.. Kurento Server

%%%%%%%%%%%%%%
Kurento Server
%%%%%%%%%%%%%%

Kurento Server Components
-------------------------

Kurento Server is composed by two components:

  - **Kurento Media Server (KMS)**: This is the heart of Kurento. It is a
    media server implemented in C/C++ using `GStreamer`:term: multimedia
    framework.

  - **Kurento Control Server (KCS)**: This component is the front-end of
    Kurento Media Server. All clients are connected to it using WebSocket
    protocol and it forward the requests to Kurento Media Server using Thrift
    protocol. It is implemented in Java 7 using Spring Boot framework.

In the following sections, we will describe in detail this two components.

Kurento Media Server
--------------------

.. todo:: Complete information about Kurento Media Server. 

Agnostic media adaptor
======================

Using the Kurento Clients, developers are able to compose the available media
elements, getting the desired pipeline. There is a challenge in this scheme, as
different media elements might require different input media formats than the
output produced by their preceding element in the chain. For example, if we
want to connect a WebRTC (VP8 encoded) or a RTP (H.264/H.263 encoded) video
stream to a face recognition media element implemented to read raw RGB format,
a transcoding is necessary.

Developers, specially during the initial phases of application development,
might want to simplify development and abstract this heterogeneneity, so
kurento provides an automatic converter of media formats called the
:term:`agnostic media adaptor <agnostic, media>`. Whenever a media element is
connected to another media element’s, Kurento verifies if media adaption and
transcoding is necessary and, in case it is, it transparently incorporates the
appropriate transformations making possible the chaining of the two elements
into the resulting pipeline.

Hence, this *agnostic media adaptor* capability fully abstracts all the
complexities of media codecs and formats. This may significantly accelerate the
development process, specially when developers are not multimedia technology
experts. However, there is a price to pay. Transcoding may be a very CPU
expensive operation. The inappropriate design of pipelines that chain media
elements in a way that unnecessarily alternate codecs (e.g. going from H.264,
to raw, to H.264 to raw again) will lead to very poor performance of
applications.

.. figure:: images/AgnosticMediaAdaptor.png
   :height: 215px
   :width:  599px
   :align:  center
   :alt:    Media Adaptor
   :figwidth: 600px

   **Media Adaptor**.

   *The agnostic media capability adapts formats between heterogeneous
   media elements making transparent for application developers all
   complexities of media representation and encoding.*

Kurento Control Server
----------------------

.. todo:: Complete information about Kurento Control Server. 







