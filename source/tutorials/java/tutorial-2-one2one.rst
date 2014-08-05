%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Tutorial 2 - One to one video call
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a one to one video call using `WebRTC`:term:
technology. In other words, this application is similar to a phone but also
with video.

The following picture shows an screenshot of this demo running in a web browser:

.. figure:: ../../images/kmf-webrtc-call-screenshot.png
   :align:   center
   :alt:     One to one video call screenshot
   :width: 600px

The interface of the application (a HTML web page) is composed by two HTML5
video tags: one for the videocamera stream (the local stream) and other for the
other peer in the call (the remote stream). If two users, A and B, are using
the application, the media flows in the following way: The videocamera stream
of user A is sent to the Kurento Media Server and sent again to the user B. On
the other hand, user B sends its videocamera stream to Kurento and then it is
sent to user A.


To implement this behavior we have to create a `Media Pipeline`:term: composed
by two WebRtc endpoints connected beetwen them. The media pipeline implemented
is illustrated in the following picture:

.. figure:: ../../images/kmf-webrtc-call-pipeline.png
   :align:   center
   :alt:     One to one video call media pipeline

To communicate the client with the server to manage calls we have designed a
signaling protocol based on `JSON <http://en.wikipedia.org/wiki/JSON>`_
messages over `WebSockets <https://www.websocket.org/>`_.

The normal sequence between client and server would be as follows:

- 1 - User A is registered in the server with his name

- 2 - User B is registered in the server with her name

- 2 - User A wants to call to User B

- 3 - User B accepts the incoming call

- 4 - The communication is established and media is flowing between User A and
  User B

- 5 - One of the users finishes the video communication

This is very simple protocol designed to show a simple one to one call
application implemented with Kurento. In a professional application it can be
improved, for example implementing seeking user, ordered finish, among other
functions.

Assuming that User A is using Client A and User B is using Client B, we can draw
the follwing sequence diagram with detailed messages between clients and server:

.. figure:: ../../images/kmf-webrtc-call-signaling.png
   :align:   center
   :alt:     One to one video call signaling protocol
   :width: 600px

As you can see in the diagram, `SDP`:term: needs to be interchanged between
client and server to establish the `WebRTC`:term: connection between the
browser and Kurento. Specifically, the SDP negotiation connects the WebRtcPeer
in the browser with the WebRtcEndpoint in the server.

The following sections describe in detail the server-side, the client-side, and
how to run the demo.

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kmf-tutorial/tree/develop/kmf-webrtc-call>`_.

Server-Side
===========

As in the :doc:`tutorial 1</tutorials/java/tutorial-1-magicmirror>`, this demo
has been developed using **Java** and
`Spring Boot <http://projects.spring.io/spring-boot/>`_.

.. note:: 

   You can use whatever Java server side technology you prefer to build web
   applications with Kurento. For example, a pure Java EE application, SIP
   Servlets, Play, Vertex, etc. We have choose Spring Boot for convenience.

The main class of this demo is named
`One2OneCallApp <https://github.com/Kurento/kmf-tutorial/blob/develop/kmf-webrtc-call/src/main/java/com/kurento/kmf/tutorial/call/CallApp.java>`_.
As you can see, the ``KurentoClient`` is instantiated in this class as a Spring
Bean.

.. sourcecode:: java

    @Configuration
    @EnableWebSocket
    @EnableAutoConfiguration
    public class One2OneCallApp implements WebSocketConfigurer {

        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(callHandler(), "/call");
        }
        
        @Bean
        public CallHandler callHandler() {
            return new CallHandler();
        }
        
        @Bean
        public UserRegistry registry() {
            return new UserRegistry();
        }

        @Bean
        public KurentoClient kurentoClient() {
            return KurentoClient.create("ws://localhost:8888");
        }

        public static void main(String[] args) throws Exception {
            new SpringApplication(One2OneCallApp.class).run(args);
        }
    }

This web application follows *Single Page Application* architecture
(`SPA <http://en.wikipedia.org/wiki/Representational_state_transfer>`_) and
uses `WebSockets <https://www.websocket.org/>`_ to communicate client with
server by means of requests and responses. Specifically, the main app class
implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHanlder`` to process web socket requests in the path ``/call``.

`CallHandler <https://github.com/Kurento/kmf-tutorial/blob/develop/kmf-webrtc-call/src/main/java/com/kurento/kmf/tutorial/call/CallHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text web socket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

In the designed protocol there are three different kind of incoming messages to
the *Server* : *register*, *call*, and *incommingCallResponse*. These messages
are treated in the *switch* clause, taking the proper steps in each case.

.. sourcecode:: java

    public class CallHandler extends TextWebSocketHandler {

        private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

        private static final Gson gson = new GsonBuilder().create();

        @Autowired
        private KurentoClient kurento;

        @Autowired
        private UserRegistry registry;

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message)
                throws Exception {
                
            JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
            
            UserSession user = registry.getBySession(session);

            if (user != null) {
                log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
            } else {
                log.debug("Incoming message from new user: {}", jsonMessage);
            }

            switch (jsonMessage.get("id").getAsString()) {
            case "register":
                register(session, jsonMessage);
                break;
            case "call":
                call(user, jsonMessage);
                break;
            case "incommingCallResponse":
                incommingCallResponse(user, jsonMessage);
                break;
            default:
                break;
            }
        }

        private void register(WebSocketSession session, JsonObject jsonMessage)
                throws IOException {
          ...  
        }
        
        private void call(WebSocketSession session, JsonObject jsonMessage)
                throws IOException {
          ...      
        }
        
        private void incommingCallResponse(WebSocketSession session, JsonObject jsonMessage)
                throws IOException {
          ...      
        }        
        
        @Override
        public void afterConnectionClosed(WebSocketSession session,
                CloseStatus status) throws Exception {
            registry.removeBySession(session);
        }

    }

In the following snippet, we can see the ``register`` method. Basically, it
obtains the ``name`` attribute from ``register`` message and check if there are
a registered user with that name. If not, the new user is registered and an
acceptance message is sent to it.

.. sourcecode :: java

   private void register(WebSocketSession session, JsonObject jsonMessage)
                throws IOException {

            String name = jsonMessage.getAsJsonPrimitive("name").getAsString();

            UserSession caller = new UserSession(session, name);
            String responseMsg = "accepted";
            if (name.isEmpty()) {
                responseMsg = "rejected: empty user name";
            } else if (registry.exists(name)) {
                responseMsg = "rejected: user '" + name + "' already registered";
            } else {
                registry.register(caller);
            }

            JsonObject response = new JsonObject();
            response.addProperty("id", "resgisterResponse");
            response.addProperty("response", responseMsg);
            caller.sendMessage(response);
        }
           
In the ``call`` method, the server checks if there are a registered user with
the name specified in ``to`` message attribute and send an ``incommingCall``
message to it. Or, if there isn't any user with that name, a ``callResponse``
message is sent to caller rejecting the call.

.. sourcecode :: java

   private void call(UserSession caller, JsonObject jsonMessage) throws IOException {
   
      String to = jsonMessage.get("to").getAsString();
      
      if (registry.exists(to)) {
         
         UserSession callee = registry.getByName(to);
         caller.setSdpOffer(jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString());
         caller.setCallingTo(to);

         JsonObject response = new JsonObject();
         response.addProperty("id", "incommingCall");
         response.addProperty("from", caller.getName());

         callee.sendMessage(response);
         
      } else {
      
         JsonObject response = new JsonObject();
         response.addProperty("id", "callResponse");
         response.addProperty("response", "rejected: user '"+to+"' is not registered");

         caller.sendMessage(response);
      }
   }

Finally, in the ``incommingCallResponse`` method, if the callee user accepts the
call, it is established and the media elements are created to connect the
caller with the callee. Basically, the server creates a ``CallMediaPipeline``
object, to encapsulate the media pipeline creation and managment. Then, this
object is used to negotiate multimedia interchange with user's browsers.

As explained in :doc:`tutorial 1</tutorials/java/tutorial-1-magicmirror>`, the
negotiation between WebRTC peer in the browser and WebRtcEndpoint in Kurento
Server is made by means of `SDP`:term: generation at the client (offer) and SDP
generation at the server (answer). The SDP answers are generated with the
Kurento Java Client inside the class ``CallMediaPipeline`` (as we see in a
moment). The methods used to generate SDP are
``generateSdpAnswerForCallee(calleeSdpOffer)`` and
``generateSdpAnswerForCaller(callerSdpOffer)``:

.. sourcecode :: java

   private void incommingCallResponse(UserSession callee, JsonObject jsonMessage) 
      throws IOException {
      
      String callResponse = jsonMessage.get("callResponse").getAsString();
      String from = jsonMessage.get("from").getAsString();
      UserSession caller = registry.getByName(from);
      String to = caller.getCallingTo();

      if ("accept".equals(callResponse)) {
      
         log.debug("Accepted call from '{}' to '{}'", from, to);

         CallMediaPipeline pipeline = new CallMediaPipeline(mpf);
         String calleeSdpOffer = jsonMessage.get("sdpOffer").getAsString();
         String calleeSdpAnswer = pipeline
               .generateSdpAnswerForCallee(calleeSdpOffer);

         JsonObject startCommunication = new JsonObject();
         startCommunication.addProperty("id", "startCommunication");
         startCommunication.addProperty("sdpAnswer", calleeSdpAnswer);
         callee.sendMessage(startCommunication);

         String callerSdpOffer = registry.getByName(from).getSdpOffer();
         String callerSdpAnswer = pipeline
               .generateSdpAnswerForCaller(callerSdpOffer);

         JsonObject response = new JsonObject();
         response.addProperty("id", "callResponse");
         response.addProperty("response", "accepted");
         response.addProperty("sdpAnswer", callerSdpAnswer);
         calleer.sendMessage(response);

      } else {
      
         JsonObject response = new JsonObject();
         response.addProperty("id", "callResponse");
         response.addProperty("response", "rejected");
         calleer.sendMessage(response);
      }
   }
           
The media logic in this demo is implemented in the class
`CallMediaPipeline <https://github.com/Kurento/kmf-tutorial/blob/develop/kmf-webrtc-call/src/main/java/com/kurento/kmf/tutorial/call/CallMediaPipeline.java>`_.
As you can see, the media pipeline of this demo is quite simple: two
``WebRtcEndpoint`` elements directly interconnected. Plase take note that the
WebRtc enpoints needs to be connected twice, one for each media direction.

.. sourcecode:: java

    public class CallMediaPipeline {

        private MediaPipeline mp;
        private WebRtcEndpoint callerWebRtcEP;
        private WebRtcEndpoint calleeWebRtcEP;

        public CallMediaPipeline(MediaPipelineFactory mpf) {
            this.mp = mpf.create();
            this.callerWebRtcEP = mp.newWebRtcEndpoint().build();
            this.calleeWebRtcEP = mp.newWebRtcEndpoint().build();

            this.callerWebRtcEP.connect(this.calleeWebRtcEP);
            this.calleeWebRtcEP.connect(this.callerWebRtcEP);
        }

        public String generateSdpAnswerForCaller(String sdpOffer) {
            return callerWebRtcEP.processOffer(sdpOffer);
        }

        public String generateSdpAnswerForCallee(String sdpOffer) {
            return calleeWebRtcEP.processOffer(sdpOffer);
        }

    }

In this class we can see the implementation of methods
``generateSdpAnswerForCaller`` and ``generateSdpAnswerForCallee``. These
methods delegate to WebRtc endpoints to create the appropriate answer.

Client-Side
===========

Let's move now to the client-side of the application. To call the previously
created WebSocket service in the server-side, we use JavaScript class
``WebSocket``. In addition, we use an specific Kurento JavaScript library
called **kurento-utils.js** to simplify the WebRTC interaction with the server.
These libraries are linked in the
`index.html <https://github.com/Kurento/kmf-tutorial/blob/develop/kmf-webrtc-call/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kmf-tutorial/blob/develop/kmf-webrtc-call/src/main/resources/static/js/index.js>`_.

In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/call``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are four incoming messages to client: *resgisterResponse*,
*callResponse*, *incommingCall*, and *startCommunication*. Convenient actions
are taken to implement each step in the communication. For example, in
functions *call* and *incommingCall* (for caller and callee respectively), the
function ``WebRtcPeer.startSendRecv`` of *kurento-utils.js* is used to start a
WebRTC communication.

.. sourcecode:: javascript

    var ws = new WebSocket('ws://' + location.host + '/call');

    ws.onmessage = function(message) {
        var parsedMessage = JSON.parse(message.data);
        console.info('Received message: ' + message.data);

        switch (parsedMessage.id) {
        case 'resgisterResponse':
            resgisterResponse(parsedMessage);
            break;
        case 'callResponse':
            callResponse(parsedMessage);
            break;
        case 'incommingCall':
            incommingCall(parsedMessage);
            break;
        case 'startCommunication':
            startCommunication(parsedMessage);
            break;
        default:
            console.error('Unrecognized message', parsedMessage);
        }
    }

    function incommingCall(message) {
        if (confirm('User ' + message.from
                + ' is calling you. Do you accept the call?')) {
            showSpinner(videoInput, videoOutput);
            webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
                    function(sdp, wp) {
                        var response = {
                            id : 'incommingCallResponse',
                            from : message.from,
                            callResponse : 'accept',
                            sdpOffer : sdp
                        };
                        sendMessage(response);
                    });
        } else {
            var response = {
                id : 'incommingCallResponse',
                from : message.from,
                callResponse : 'reject'
            };
            sendMessage(response);
            stop();
        }
    }

    function call() {
        showSpinner(videoInput, videoOutput);

        kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(
                offerSdp, wp) {
            webRtcPeer = wp;
            console.log('Invoking SDP offer callback function');
            var message = {
                id : 'call',
                from : document.getElementById('name').value,
                to : document.getElementById('peer').value,
                sdpOffer : offerSdp
            };
            sendMessage(message);
        });
    }

Dependencies
============

This Java Spring application is implementad using
`Maven <http://maven.apache.org/>`_. The relevant part of the *pom.xml* is
where Kurento dependencies are declared. As the following snippet shows, we
need two dependencies: the Kurento Client Java dependency (*kurento-client*)
and the JavaScript Kurento utility library (*kurento-utils*) for the
client-side:

.. sourcecode:: xml 

   <dependencies> 
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-client</artifactId>
         <version>0.9.0</version>
      </dependency> 
      <dependency> 
         <groupId>org.kurento</groupId>
         <artifactId>kurento-utils-js</artifactId> 
         <version>0.9.0</version>
      </dependency> 
   </dependencies>


How to run this application
===========================

First of all, you should install Kurento Server to run this demo. Please visit
the `installation guide <../../Installation_Guide.rst>`_ for further
information.

This demo is assuming that you have a Kurento Server installed and running in
your local machine. If so, to launch the app you need to clone the GitHub
project where this demo is hosted, and then run the main class, as follows:

.. sourcecode:: shell

    git clone https://github.com/Kurento/kurento-java-tutorial.git
    cd tutorial-2-One2OneCall
    mvn exec:java -Dexec.mainClass="org.kurento.tutorial.one2one.One2OneCallApp"

The web application starts on port 8080 in the localhost by default. Therefore,
open the URL http://localhost:8080/ in a WebRTC compliant browser (Chrome,
Firefox).