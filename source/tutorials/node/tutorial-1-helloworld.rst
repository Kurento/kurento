%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Tutorial 1 - Hello world
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application has been designed to introduce the principles of
programming with Kurento for Node.js developers. It consists on a `WebRTC`:term:
video communication in mirror (*loopback*). This tutorial assumes you have
basic knowledge on JavaScript, Node.js, HTML and WebRTC. We also recommend reading the
:doc:`Introducing Kurento <../../introducing_kurento>` section before starting
this tutorial.

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example.
Read the `installation guide <../../Installation_Guide.rst>`_ for further
information.

Be sure to have installed `Node.js`:term: in your system. In an Ubuntu machine,
you can install it with:

.. sourcecode:: sh

   sudo add-apt-repository ppa:chris-lea/node.js
   sudo apt-get update
   sudo apt-get install nodejs

Also be sure to have installed `Bower`:term: in your system:

.. sourcecode:: sh

   sudo npm install -g bower

To launch the application you need to clone the GitHub project where this demo
is hosted and then install and run it, as follows:

.. sourcecode:: shell

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-hello-world
    npm install
    cd static
    bower install
    cd ..
    node app.js

Access the application connecting to the URL http://localhost:8080/ through a
WebRTC capable browser (Chrome, Firefox).


Understanding this example
==========================

Kurento provides developers a **Kurento JavaScript Client** to control
**Kurento Media Server**. This client library can be used from compatible
JavaScript engines including browsers and Node.js.

This *hello world* demo is one of the simplest web application you can create
with Kurento. The following picture shows an screenshot of this demo running:

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-screenshot.png 
   :align:   center
   :alt:     Kurento Hello World Screenshot: WebRTC in loopback
   :width: 600x

   *Kurento Hello World Screenshot: WebRTC in loopback*

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one showing the local stream (as captured by the device webcam) and
the other showing the remote stream sent by the media server back to the client.

The logic of the application is quite simple: the local stream is sent to the
Kurento Media Server, which returns it back to the client without
modifications. To implement this behavior we need to create a
`Media Pipeline`:term: composed by a single `Media Element`:term:, i.e. a
**WebRtcEndpoint**, which holds the capability of exchanging full-duplex
(bidirectional) WebRTC media flows. This media element is connected to itself
so that the media it receives (from browser) is send back (to browser). This
media pipeline is illustrated in the following picture:


.. figure:: ../../images/kurento-java-tutorial-1-helloworld-pipeline.png
   :align:   center
   :alt:     Kurento Hello World Media Pipeline in context

   *Kurento Hello World Media Pipeline in context*

This is a web application, and therefore it follows a client-server
architecture. At the client-side, the logic is implemented in **JavaScript**.
At the server-side we use a Node.js application server consuming the
**Kurento JavaScript Client** API to control **Kurento Media Server** capabilities.
All in all, the high level architecture of this demo is three-tier. To
communicate these entities the following technologies are used:

* `REST`:term:: Communication between JavaScript client-side and Node.js
  application server-side.

* `WebSocket`:term:: Communication between the Kurento JavaScript Client and the
  Kurento Media Server. This communication is implemented by the
  **Kurento Protocol**. For further information, please see this
  :doc:`page <../../mastering/kurento_protocol>` of the documentation.

The diagram below shows an complete sequence diagram from the interactions with
the application interface to: i) JavaScript logic; ii) Application server logic
(which uses the Kurento JavaScript Client); iii) Kurento Media Server.

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-signaling.png
   :align:   center
   :alt:     Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo
   :width: 600px

   *Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo*

.. note::

   The communication between client and server-side does not need to be
   REST. For simplicity, in this tutorial REST has been used. In later examples
   a more complex signaling between client and server has been implement,
   using WebSockets. Please see later tutorials for further information.

The following sections analyze in deep the server and client-side
code of this application. The complete source code can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-node/tree/master/kurento-hello-world>`_.


Application Server Logic
========================

This demo has been developed using the **express** framework for Node.js, but
express is not a requirement for Kurento.

The main script of this demo is
`app.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/app.js>`_.
As you can see, the *KurentoClient* is instantiated in this class. In this 
instantiation we see that we need to specify to the client library the location 
of the Kurento Media Server. In this example, we assume it's located at *localhost* 
listening in port 8888. If you reproduce this example you'll need to insert the specific
location of your Kurento Media Server instance there.

Once the *Kurento Client* has been instantiated, you are ready for communicating
with Kurento Media Server and controlling its multimedia capabilities. Our first
operation is to create a *Media Pipeline*.

.. sourcecode:: js

   var kurento = require('kurento-client');

   //...

   const ws_uri = "ws://localhost:8888/kurento";

   //...

   kurento(ws_uri, function(error, kurentoClient) {
	if (error) {
		return callback(error);
	}
	kurentoClient.create('MediaPipeline', function(error, _pipeline) {
		if (error) {
			return callback(error);
		}
		pipeline = _pipeline;
		return callback(null, pipeline);
	});
   });

As introduced before, we use `REST`:term: to communicate the client with the
Node.js application server:

.. sourcecode:: js

  app.post('/helloworld', function(req, res) {
        var sdpOffer = req.body;

        getPipeline(function(error, pipeline) {

                pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {

                        webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {

                                webRtcEndpoint.connect(webRtcEndpoint, function(error) {

                                        res.type('application/sdp');
                                        res.send(sdpAnswer);

                                });
                        });
                });
        });
});

As it can be observed, when a  POST requests arrives to path */helloworld*, we
execute a logic comprising two steps:

 - **Configure media processing logic**: This is the part in which the
   application configures how Kurento has to process the media. In other words,
   the media pipeline is recovered and, using it, the media elements we
   need are created and connected. In this case, we only instantiate one
   *WebRtcEndpoint* for receiving the WebRTC stream and sending it back to the
   client.

 - **WebRTC SDP negotiation**: In WebRTC, an `SDP`:term: (Session Description
   protocol) is used for negotiating media exchanges between apps. Such
   negotiation happens based on the SDP offer and answer exchange mechanism. In
   this example we assume the SDP offer and answer contain all WebRTC ICE
   candidates. This negotiation takes place when invoking
   *processOffer*, using the SDP offer obtained from the browser client and
   returning a SDP answer generated by WebRtcEndpoint.


Client-Side Logic
=================

Let's move now to the client-side of the application, which follows
*Single Page Application* architecture (`SPA`:term:). To call the previously
created REST service, we use the JavaScript library `jQuery`:term:. In
addition, we use a Kurento JavaScript utilities library called
*kurento-utils.js* to simplify the WebRTC management in the browser.

These libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/static/js/index.js>`_.
In the *start* function we can see how jQuery is used to send a POST request to
the path */helloworld*, where the application server REST service is listening.
The function *WebRtcPeer.startSendRecv* abstracts the WebRTC internal details
(i.e. PeerConnection and getUserStream) and makes possible to start a
full-duplex WebRTC communication, using the HTML video tag with id *videoInput*
to show the video camera (local stream) and the video tag *videoOutput* to show
the remote stream provided by the Kurento Media Server.

.. sourcecode:: javascript

   var webRtcPeer;

   function start() {
      console.log("Starting video call ...");
      showSpinner(videoInput, videoOutput);
      webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);
   }

   function onOffer(sdpOffer) {
      console.info('Invoking SDP offer callback function ' + location.host);
      $.ajax({
         url : location.protocol + '/helloworld',
         type : 'POST',
         dataType : 'text',
         contentType : 'application/sdp',
         data : sdpOffer,
         success : function(sdpAnswer) {
            console.log("Received sdpAnswer from server. Processing ...");
            webRtcPeer.processSdpAnswer(sdpAnswer);
         },
         error : function(jqXHR, textStatus, error) {
            onError(error);
         }
      });
   }

   function onError(error) {
      console.error(error);
   }


Dependencies
============

Dependencies of this demo are managed using npm. Our main dependency is the
Kurento Client JavaScript (*kurento-client*). The relevant part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/package.json>` file for managing this dependency is:

.. sourcecode:: json

  "dependencies": {
     ...
     "kurento-client" : "|version|"
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/static/js/bower.js>` file and pay attention to the following section:

.. sourcecode:: json

  "dependencies": {
     "kurento-utils" : "|version|"
   }

.. note::

   We are in active development. Be sure that you have the latest version of Kurento 
   JavaScript Client in your POM. You can find it at nom searching for ``kurento-client``.
