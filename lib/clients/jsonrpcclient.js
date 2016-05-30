/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var RpcBuilder = require('../..');
var WebSocketWithReconnection = require('./transports/webSocketWithReconnection');

Date.now = Date.now || function() {
    return +new Date;
};

var PING_INTERVAL = 5000;

var RECONNECTING = 'RECONNECTING';
var CONNECTED = 'CONNECTED';
var DISCONNECTED = 'DISCONNECTED';

var RECONNECTING = "RECONNECTING";
var CONNECTED = "CONNECTED";
var DISCONNECTED = "DISCONNECTED";


/**
 * 
 * heartbeat: interval in ms for each heartbeat message,
 * sendCloseMessage : true / false, before closing the connection, it sends a closeSession message
 * <pre>
 * ws : {
 * 	uri : URI to conntect to,
 *  useSockJS : true (use SockJS) / false (use WebSocket) by default,
 * 	onconnected : callback method to invoke when connection is successful,
 * 	ondisconnect : callback method to invoke when the connection is lost,
 * 	onreconnecting : callback method to invoke when the client is reconnecting,
 * 	onreconnected : callback method to invoke when the client succesfully reconnects,
 * },
 * rpc : {
 * 	requestTimeout : timeout for a request,
 * 	sessionStatusChanged: callback method for changes in session status,
 * 	mediaRenegotiation: mediaRenegotiation
 * }
 * </pre>
 */
function JsonRpcClient(configuration) {

    var self = this;

    var wsConfig = configuration.ws;

    var notReconnectIfNumLessThan = -1;

    var pingNextNum = 0;
    var enabledPings = true;
    var pingPongStarted = false;
    var pingInterval;

    var status = DISCONNECTED;

    var onreconnecting = wsConfig.onreconnecting;
    var onreconnected = wsConfig.onreconnected;
    var onconnected = wsConfig.onconnected;

    configuration.rpc.pull = function(params, request) {
        request.reply(null, "push");
    }

    wsConfig.onreconnecting = function() {
        console.log("--------- ONRECONNECTING -----------");
        if (status === RECONNECTING) {
            console.error("Websocket already in RECONNECTING state when receiving a new ONRECONNECTING message. Ignoring it");
            return;
        }

        status = RECONNECTING;
        if (onreconnecting) {
            onreconnecting();
        }
    }

    wsConfig.onreconnected = function() {
        console.log("--------- ONRECONNECTED -----------");
        if (status === CONNECTED) {
            console.error("Websocket already in CONNECTED state when receiving a new ONRECONNECTED message. Ignoring it");
            return;
        }
        status = CONNECTED;

        enabledPings = true;
        updateNotReconnectIfLessThan();
        usePing();

        if (onreconnected) {
            onreconnected();
        }
    }

    wsConfig.onconnected = function() {
        console.log("--------- ONCONNECTED -----------");
        if (status === CONNECTED) {
            console.error("Websocket already in CONNECTED state when receiving a new ONCONNECTED message. Ignoring it");
            return;
        }
        status = CONNECTED;

        enabledPings = true;
        usePing();

        if (onconnected) {
            onconnected();
        }
    }

    var ws = new WebSocketWithReconnection(wsConfig);

    console.log('Connecting websocket to URI: ' + wsConfig.uri);

    var rpcBuilderOptions = {
        request_timeout: configuration.rpc.requestTimeout
    };

    var rpc = new RpcBuilder(RpcBuilder.packers.JsonRPC, rpcBuilderOptions, ws,
        function(request) {

            console.log('Received request: ' + JSON.stringify(request));

            try {
                var func = configuration.rpc[request.method];

                if (func === undefined) {
                    console.error("Method " + request.method + " not registered in client");
                } else {
                    func(request.params, request);
                }
            } catch (err) {
                console.error('Exception processing request: ' + JSON.stringify(request));
                console.error(err);
            }
        });

    this.send = function(method, params, callback) {
        if (method !== 'ping') {
            console.log('Request: method:' + method + " params:" + JSON.stringify(params));
        }

        var requestTime = Date.now();

        rpc.encode(method, params, function(error, result) {
            if (error) {
                try {
                    console.error("ERROR:" + error.message + " in Request: method:" + method + " params:" + JSON.stringify(params));
                    if (error.data) {
                        console.error("ERROR DATA:" + JSON.stringify(error.data));
                    }
                } catch (e) {}
                error.requestTime = requestTime;
            }
            if (callback) {
                if (result != undefined && result.message !== 'pong') {
                    console.log('Response: ' + JSON.stringify(result));
                }
                callback(error, result);
            }
        });
    }

    function updateNotReconnectIfLessThan() {
        notReconnectIfNumLessThan = pingNextNum;
        console.log("notReconnectIfNumLessThan = " + notReconnectIfNumLessThan);
    }

    function sendPing() {
        if (enabledPings) {
            var params = null;

            if (pingNextNum == 0 || pingNextNum == notReconnectIfNumLessThan) {
                params = {
                    interval: PING_INTERVAL
                };
            }

            pingNextNum++;

            self.send('ping', params, (function(pingNum) {
                return function(error, result) {
                    if (error) {
                        if (pingNum > notReconnectIfNumLessThan) {
                            enabledPings = false;
                            updateNotReconnectIfLessThan();
                            console.log("DSS did not respond to ping message " + pingNum + ". Reconnecting... ");
                            ws.reconnectWs();
                        }
                    }
                }
            })(pingNextNum));
        } else {
            console.log("Trying to send ping, but ping is not enabled");
        }
    }

    /*
    * If configuration.hearbeat has any value, the ping-pong will work with the interval
    * of configuration.hearbeat
    */
    function usePing() {
        if (!pingPongStarted) {
            console.log("Starting ping (if configured)")
            pingPongStarted = true;

            if (configuration.heartbeat != undefined) {
                pingInterval = setInterval(sendPing, configuration.heartbeat);
                sendPing();
            }
        }
    }

    this.close = function() {
        console.log("Closing jsonRpcClient explicitely by client");

        if (pingInterval != undefined) {
            clearInterval(pingInterval);
        }
        pingPongStarted = false;
        enabledPings = false;

        if (configuration.sendCloseMessage) {
            this.send('closeSession', null, function(error, result) {
                if (error) {
                    console.error("Error sending close message: " + JSON.stringify(error));
                }

                ws.close();
            });
        } else {
			ws.close();
        }
    }

    // This method is only for testing
    this.forceClose = function(millis) {
        ws.forceClose(millis);
    }

    this.reconnect = function() {
        ws.reconnectWs();
    }
}


module.exports = JsonRpcClient;
