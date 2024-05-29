==================
Welcome to Kurento
==================

:term:`Kurento` Media Server (**KMS**) is a multimedia server package that can be used to develop advanced video applications for :term:`WebRTC` platforms. It is an Open Source project, with source code released under the terms of `Apache License Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0>`__ and `available on GitHub <https://github.com/Kurento>`__.

.. warning::

   **This project is on bare minimum maintenance mode**.

   **There are no major new features planned for Kurento, and even minor issues may take some time to be addressed**.

   Kurento won't implement several WebRTC features such as Simulcast, End-To-End Encryption, Insertable Streams, or even support for more than 1 video + 1 audio in the same WebRTC peer connection.

   For new videoconferencing projects we recommend to build on top of a higher-level platform such as `OpenVidu <https://openvidu.io/>`__ (from the same team as Kurento). It hides to some extent the sheer complexity of scalable WebRTC systems, and allows you to focus on your app instead.

   If you're just looking for a bare-bones, low-level WebRTC SFU like Kurento, `mediasoup <https://mediasoup.org/>`__ is a very good, modern and actively developed alternative.

**Start here**: :doc:`/user/intro` and :doc:`/user/quickstart`, and then learn to write Kurento applications with :doc:`/user/tutorials`.

The main documentation for the project is organized into different sections:

- :ref:`user-docs`
- :ref:`feature-docs`
- :ref:`project-docs`

Information about *development of Kurento itself* is also available:

- :doc:`/project/relnotes/index`
- :ref:`dev-docs`



.. _user-docs:

.. toctree::
   :maxdepth: 2
   :caption: User Documentation

   /user/intro
   /user/openvidu
   /user/quickstart
   /user/installation
   /user/installation_dev
   /user/configuration
   /user/tutorials
   /user/writing_applications
   /user/writing_modules
   /user/faq
   /user/troubleshooting
   /user/support



.. _feature-docs:

.. toctree::
   :maxdepth: 2
   :caption: Feature Documentation

   /features/kurento_client
   /features/kurento_modules
   /features/kurento_protocol
   /features/kurento_utils_js
   /features/security
   /features/events
   /features/nat_traversal
   /features/statistics
   /features/logging



.. _project-docs:

.. toctree::
   :maxdepth: 2
   :caption: Project Documentation

   /project/team
   /project/contributing
   /project/conduct
   /project/relnotes/index

..
   Commented out for now:
   /project/sponsors
   /project/opensource
   /project/story



.. _dev-docs:

.. toctree::
   :maxdepth: 2
   :caption: Developer Documentation

   /dev/dev_guide
   /dev/ci
   /dev/release
   /dev/hardening
   /dev/writing_documentation
   /dev/testing

..
   Commented out for now:
   /dev/changelog
   /dev/architecture



.. _knowledge-docs:

.. toctree::
   :maxdepth: 2
   :caption: Knowledge Base

   /knowledge/browser
   /knowledge/congestion_rmcat
   /knowledge/h264
   /knowledge/memory_fragmentation
   /knowledge/mp4
   /knowledge/nat
   /knowledge/rtp_streaming
   /knowledge/safari
   /knowledge/selfsigned_certs
   /glossary



Indices and tables
==================

- :ref:`genindex`
- :ref:`search`
