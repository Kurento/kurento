/*
 * (C) Copyright 2026 Kurento (http://kurento.org/)
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
 */

function getopts(args, opts)
{
  var result = opts.default || {};
  args.replace(
      new RegExp('([^?=&]+)(=([^&]*))?', 'g'),
      function($0, $1, $2, $3) { result[$1] = decodeURI($3); });

  return result;
}

var args = getopts(location.search,
{
  default:
  {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    relay_url: 'https://relay.example.com',
    broadcast: 'kurento-demo.hang',
    ice_servers: undefined
  }
});

var publisher = {
  client: null,
  pipeline: null,
  endpoint: null,
  bridge: null,
  webRtcPeer: null
};

var subscriber = {
  client: null,
  pipeline: null,
  endpoint: null,
  bridge: null,
  webRtcPeer: null
};

function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror)
{
  webRtcPeer.on('icecandidate', function(candidate) {
    console.log('Local candidate:', candidate);

    candidate = kurentoClient.getComplexType('IceCandidate')(candidate);
    webRtcEp.addIceCandidate(candidate, onerror);
  });

  webRtcEp.on('IceCandidateFound', function(event) {
    var candidate = event.candidate;

    console.log('Remote candidate:', candidate);
    webRtcPeer.addIceCandidate(candidate, onerror);
  });
}

function getIceConfiguration()
{
  if (!args.ice_servers) {
    console.log('Use freeice');
    return undefined;
  }

  console.log('Use ICE servers: ' + args.ice_servers);
  return {
    iceServers: JSON.parse(args.ice_servers)
  };
}

function getFormValues()
{
  return {
    wsUri: document.getElementById('wsUri').value,
    relayUrl: document.getElementById('relayUrl').value,
    broadcastName: document.getElementById('broadcastName').value
  };
}

function setStatus(elementId, level, text)
{
  var element = document.getElementById(elementId);
  element.className = 'alert alert-' + level + ' status-box';
  element.textContent = text;
}

function setButtons()
{
  document.getElementById('startPublish').className = publisher.webRtcPeer ? 'btn btn-success disabled' : 'btn btn-success';
  document.getElementById('stopPublish').className = publisher.webRtcPeer ? 'btn btn-danger' : 'btn btn-danger disabled';
  document.getElementById('startSubscribe').className = subscriber.webRtcPeer ? 'btn btn-primary disabled' : 'btn btn-primary';
  document.getElementById('stopSubscribe').className = subscriber.webRtcPeer ? 'btn btn-danger' : 'btn btn-danger disabled';
}

function disposeSide(side, stopMethod, statusId, idleText, videoId)
{
  var bridge = side.bridge;
  var pipeline = side.pipeline;
  var client = side.client;
  var webRtcPeer = side.webRtcPeer;

  function finalize() {
    if (pipeline) {
      pipeline.release();
    }

    if (webRtcPeer) {
      webRtcPeer.dispose();
    }

    if (client && client.close) {
      client.close();
    }

    side.client = null;
    side.pipeline = null;
    side.endpoint = null;
    side.bridge = null;
    side.webRtcPeer = null;

    document.getElementById(videoId).src = '';
    setStatus(statusId, 'info', idleText);
    setButtons();
  }

  if (bridge && typeof bridge[stopMethod] === 'function') {
    bridge[stopMethod](function(error) {
      if (error) {
        console.warn(error);
      }
      finalize();
    });
  } else {
    finalize();
  }
}

function stopPublisher()
{
  disposeSide(publisher, 'unpublish', 'publisherStatus', 'Publisher idle.', 'videoInput');
}

function stopSubscriber()
{
  disposeSide(subscriber, 'unsubscribe', 'subscriberStatus', 'Subscriber idle.', 'videoOutput');
}

function onError(error)
{
  if (error) {
    console.error(error);
  }
}

function startPublisher()
{
  var values;
  var options;

  if (publisher.webRtcPeer) {
    return;
  }

  values = getFormValues();
  if (!values.wsUri || !values.relayUrl || !values.broadcastName) {
    setStatus('publisherStatus', 'warning', 'Set KMS WebSocket, relay URL, and broadcast name first.');
    return;
  }

  options = {
    localVideo: document.getElementById('videoInput'),
    configuration: getIceConfiguration(),
    mediaConstraints: {
      audio: true,
      video: true
    }
  };

  setStatus('publisherStatus', 'info', 'Creating WebRTC publisher...');

  publisher.webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options, function(error) {
    if (error) {
      publisher.webRtcPeer = null;
      setButtons();
      return onError(error);
    }

    this.generateOffer(function(offerError, sdpOffer) {
      if (offerError) {
        stopPublisher();
        return onError(offerError);
      }

      kurentoClient(values.wsUri, function(clientError, client) {
        if (clientError) {
          stopPublisher();
          return onError(clientError);
        }

        publisher.client = client;

        client.create('MediaPipeline', function(pipelineError, pipeline) {
          var elements;

          if (pipelineError) {
            stopPublisher();
            return onError(pipelineError);
          }

          publisher.pipeline = pipeline;
          elements = [
            {type: 'WebRtcEndpoint', params: {}},
            {
              type: 'MoqPublisherEndpoint',
              params: {
                url: values.relayUrl,
                broadcast: values.broadcastName
              }
            }
          ];

          pipeline.create(elements, function(elementError, createdElements) {
            if (elementError) {
              stopPublisher();
              return onError(elementError);
            }

            publisher.endpoint = createdElements[0];
            publisher.bridge = createdElements[1];

            setIceCandidateCallbacks(publisher.webRtcPeer, publisher.endpoint, function(iceError) {
              if (iceError) {
                stopPublisher();
                onError(iceError);
              }
            });

            publisher.endpoint.processOffer(sdpOffer, function(processError, sdpAnswer) {
              if (processError) {
                stopPublisher();
                return onError(processError);
              }

              publisher.webRtcPeer.processAnswer(sdpAnswer, function(answerError) {
                if (answerError) {
                  stopPublisher();
                  return onError(answerError);
                }

                publisher.endpoint.gatherCandidates(function(gatherError) {
                  if (gatherError) {
                    stopPublisher();
                    return onError(gatherError);
                  }

                  client.connect(publisher.endpoint, publisher.bridge, function(connectError) {
                    if (connectError) {
                      stopPublisher();
                      return onError(connectError);
                    }

                    publisher.bridge.publish(function(publishError) {
                      if (publishError) {
                        stopPublisher();
                        return onError(publishError);
                      }

                      setStatus('publisherStatus', 'success', 'Publishing to ' + values.relayUrl + ' as ' + values.broadcastName + '.');
                      setButtons();
                    });
                  });
                });
              });
            });
          });
        });
      });
    });
  });

  setButtons();
}

function startSubscriber()
{
  var values;
  var options;

  if (subscriber.webRtcPeer) {
    return;
  }

  values = getFormValues();
  if (!values.wsUri || !values.relayUrl || !values.broadcastName) {
    setStatus('subscriberStatus', 'warning', 'Set KMS WebSocket, relay URL, and broadcast name first.');
    return;
  }

  options = {
    remoteVideo: document.getElementById('videoOutput'),
    configuration: getIceConfiguration()
  };

  setStatus('subscriberStatus', 'info', 'Creating WebRTC subscriber...');

  subscriber.webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function(error) {
    if (error) {
      subscriber.webRtcPeer = null;
      setButtons();
      return onError(error);
    }

    this.generateOffer(function(offerError, sdpOffer) {
      if (offerError) {
        stopSubscriber();
        return onError(offerError);
      }

      kurentoClient(values.wsUri, function(clientError, client) {
        if (clientError) {
          stopSubscriber();
          return onError(clientError);
        }

        subscriber.client = client;

        client.create('MediaPipeline', function(pipelineError, pipeline) {
          var elements;

          if (pipelineError) {
            stopSubscriber();
            return onError(pipelineError);
          }

          subscriber.pipeline = pipeline;
          elements = [
            {type: 'WebRtcEndpoint', params: {}},
            {
              type: 'MoqSubscriberEndpoint',
              params: {
                url: values.relayUrl,
                broadcast: values.broadcastName
              }
            }
          ];

          pipeline.create(elements, function(elementError, createdElements) {
            if (elementError) {
              stopSubscriber();
              return onError(elementError);
            }

            subscriber.endpoint = createdElements[0];
            subscriber.bridge = createdElements[1];

            subscriber.bridge.on('EndOfStream', function() {
              setStatus('subscriberStatus', 'warning', 'The relay closed the broadcast.');
              stopSubscriber();
            });

            setIceCandidateCallbacks(subscriber.webRtcPeer, subscriber.endpoint, function(iceError) {
              if (iceError) {
                stopSubscriber();
                onError(iceError);
              }
            });

            subscriber.endpoint.processOffer(sdpOffer, function(processError, sdpAnswer) {
              if (processError) {
                stopSubscriber();
                return onError(processError);
              }

              subscriber.webRtcPeer.processAnswer(sdpAnswer, function(answerError) {
                if (answerError) {
                  stopSubscriber();
                  return onError(answerError);
                }

                subscriber.endpoint.gatherCandidates(function(gatherError) {
                  if (gatherError) {
                    stopSubscriber();
                    return onError(gatherError);
                  }

                  client.connect(subscriber.bridge, subscriber.endpoint, function(connectError) {
                    if (connectError) {
                      stopSubscriber();
                      return onError(connectError);
                    }

                    subscriber.bridge.subscribe(function(subscribeError) {
                      if (subscribeError) {
                        stopSubscriber();
                        return onError(subscribeError);
                      }

                      setStatus('subscriberStatus', 'success', 'Subscribed to ' + values.broadcastName + ' from ' + values.relayUrl + '.');
                      setButtons();
                    });
                  });
                });
              });
            });
          });
        });
      });
    });
  });

  setButtons();
}

window.addEventListener('load', function()
{
  console = new Console();

  document.getElementById('wsUri').value = args.ws_uri;
  document.getElementById('relayUrl').value = args.relay_url;
  document.getElementById('broadcastName').value = args.broadcast;

  document.getElementById('startPublish').addEventListener('click', function(event) {
    event.preventDefault();
    startPublisher();
  });

  document.getElementById('stopPublish').addEventListener('click', function(event) {
    event.preventDefault();
    stopPublisher();
  });

  document.getElementById('startSubscribe').addEventListener('click', function(event) {
    event.preventDefault();
    startSubscriber();
  });

  document.getElementById('stopSubscribe').addEventListener('click', function(event) {
    event.preventDefault();
    stopSubscriber();
  });

  setButtons();
});
