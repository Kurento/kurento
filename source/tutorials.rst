%%%%%%%%%%%%%%%%%
Kurento Tutorials
%%%%%%%%%%%%%%%%%

This section contains tutorials showing how to use Kurento framework to build
different types of `WebRTC`:term: and multimedia applications. Turorials come in
three flavours:

- **Browser JavaScript**: These show applications executing at the browser
and communicating directly with the Kurento Media Server. In these tutorial,
all the application logic is hold by the browser. Hence, no application server
is necessary. For these reasons, these applications need to be simple.

- **Node.js**: These show applications where clients interact with an
application server based on Node.js technology. The application server holds
the logic orchestrating the communication among the clients and 
controling Kurento Server capabilities for them.

- **Java**: These show applications where clients interact with an
application server based on Java EE technology. The application server hold
the logic orchestrating the communication among the clients and
controling Kurento Server capabilities for them.

**Disclaimer** These tutorials have been created with learning objectives.
They are not intended to be used in production environments where different
unmanaged error conditions may emerge.


Tutorial 1 - Hello world
========================

This is one the simplest WebRTC application you can create with Kurento. It
implements a `WebRTC`:term: *loopback* (a WebRTC media stream going from client to
Kurento and back to the client)

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-1-helloworld>
   Browser JavaScript </tutorials/js/tutorial-1-helloworld>
   Node.js </tutorials/node/tutorial-1-helloworld>

Tutorial 2 - WebRTC magic mirror
================================

This web application consists on a `WebRTC`:term: video communication in mirror
adding a funny hat over your face. This is an example of computer vision
and augmented reality using a filter.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-2-magicmirror>
   Browser JavaScript </tutorials/js/tutorial-2-magicmirror>
   Node.js </tutorials/node/tutorial-2-magicmirror>

Tutorial 3 - WebRTC one-to-many broadcast
===================================

Video broadcasting for `WebRTC`:term:. One peer
transmits a video stream and N peers receives it.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-3-one2many>
..   Browser JavaScript </tutorials/js/tutorial-3-one2many>
   Node.js </tutorials/node/tutorial-3-one2many>

Tutorial 4 - WebRTC one-to-one video call
==================================

This web application is a videophone (call one to one) based on `WebRTC`:term:.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-4-one2one>
..   Browser JavaScript </tutorials/js/tutorial-4-one2one>
   Node.js </tutorials/node/tutorial-4-one2one>

Tutorial 5 - WebRTC one-to-one video call with recording and filtering
===============================================================

This is an enhanced version of the previous application recording of the
video communication, and also integration with an augmented reality filter.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-5-one2one-adv>
   Browser JavaScript </tutorials/js/tutorial-5-one2one-adv>
   Node.js </tutorials/node/tutorial-5-one2one-adv>

.. 
   Tutorial 6 - Group video call
   =============================

   Web application based on WebRTC to communicate several peers.

   .. toctree::
      :maxdepth: 1

      Java </tutorials/java/tutorial-6-group>
      Browser JavaScript </tutorials/js/tutorial-6-group>
      Node.js </tutorials/node/tutorial-6-group>

   Tutorial 7 - Group video call with recording and filtering
   ==========================================================

   Enhanced version of the previous demo, with recording and filtering capabilities.

   Tutorial: Java, Node.js, Browser JavaScript


   Tutorial 8 - HTTP Player
   ========================

   Play of a video through HTTP.

   Tutorial: Java, Node.js, Browser JavaScript
