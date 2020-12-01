===============
Getting Started
===============

Generally speaking, these are the first steps that any user interested in Kurento should follow:

1. **Know your use case**

   Choose between Kurento and `OpenVidu`_.

   :term:`Kurento Media Server` has been designed as a general-purpose platform that can be used to create any kind of multimedia streaming applications. This makes KMS a powerful tool, however it also means that there is some unavoidable complexity that the developer must face.

   :term:`WebRTC` is a complex standard with a lot of moving parts, and you need to know about each one of these components and how they work together to achieve the multimedia communications that the standard strives to offer.

   If your intended application consists of a complex setup with different kinds of sources and varied use cases, then Kurento is the best leverage you can use.

   However, if you intend to solve a simpler use case, such as those of video conference applications, the `OpenVidu`_ project builds on top of Kurento to offer a simpler and easier to use solution that will save you time and development effort.

2. **Install KMS**

   The :doc:`installation guide </user/installation>` explains different ways in which Kurento can be installed in your system. The fastest and easiest one is to use our :ref:`pre-configured template for Amazon AWS <installation-aws>`.

3. **Configure KMS**

   KMS is able to run as-is after a normal installation. However, there are several parameters that you might want to tune in the :doc:`configuration files </user/configuration>`.

4. **Write an Application**

   Write an application that uses the :doc:`/features/kurento_protocol` to comunicate with KMS and use its capabilities. The easiest way of doing this is to build on one of the provided :doc:`Kurento Clients </features/kurento_client>`.

   Have a look at any of the multiple :doc:`tutorials </user/tutorials>` that explain how to build basic applications.

5. **Ask for help**

   If you face any issue with Kurento itself or have difficulties configuring the plethora of mechanisms that form part of WebRTC, don't hesitate to :doc:`ask for help </user/support>` to our community of users.

   Still, there are times when the problems at hand require more specialized study. If you wish to get help from expert people with more inside knowledge of Kurento, get in contact with us to request :ref:`Commercial Support <support-commercial>`.

6. **Enjoy!**

   Kurento is a project that aims to bring the latest innovations closer to the people, and help connect them together. Make a great application with it, and let us know! We will be more than happy to find out about who is using Kurento and what is being built with it :-)

.. _OpenVidu: https://openvidu.io/
