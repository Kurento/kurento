/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

/**
 * {@link WebRtcPeer} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#getLocalSessionDescriptor()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link HttpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
 * @since 4.2.4
 *
 */

if(typeof QUnit == 'undefined')
{
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoUtils = require('..');

  require('./_common');
};


var WebRtcPeerRecvonly = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly;
var WebRtcPeerSendonly = kurentoUtils.WebRtcPeer.WebRtcPeerSendonly;
var WebRtcPeerSendrecv = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv;

var mediaConstraints =
{
  audio: false,
  video:
  {
    mandatory:
    {
      maxWidth: 640,
      maxFrameRate: 15,
      minFrameRate: 15
    }
  }
};


QUnit.module('WebRtcPeer');

QUnit.test('WebRtcPeerRecvonly', function(assert)
{
  var done = assert.async();

  assert.expect(1);

//  var localVideo  = document.getElementById('localVideo')
//  var remoteVideo = document.getElementById('remoteVideo')

  var webRtcPeer = new WebRtcPeerRecvonly()

  function onerror(error)
  {
    webRtcPeer.dispose()

    _onerror(error)
  }

  webRtcPeer.on('error', onerror)
  webRtcPeer.on('sdpoffer', function(sdpOffer)
  {
    var offer = new RTCSessionDescription(
    {
      type: 'offer',
      sdp:  sdpOffer
    });

    var peerConnection = new RTCPeerConnection()

    peerConnection.setRemoteDescription(offer, function()
    {
      getUserMedia(mediaConstraints, function(stream)
      {
        peerConnection.addStream(stream)

        peerConnection.createAnswer(function(answer)
        {
          peerConnection.setLocalDescription(answer, function()
          {
            webRtcPeer.processSdpAnswer(answer.sdp, function(error)
            {
              if(error) return onerror(error)

              var stream = this.getRemoteStream()
              assert.notEqual(stream, undefined, 'remote stream')

              this.dispose()
              done()
            })
          },
          onerror);
        },
        onerror);
      },
      onerror);
    },
    onerror)
  })
});

QUnit.test('WebRtcPeerSendonly', function(assert)
{
  var done = assert.async();

  assert.expect(2);

//  var localVideo  = document.getElementById('localVideo')
//  var remoteVideo = document.getElementById('remoteVideo')

  var webRtcPeer = new WebRtcPeerSendonly()

  function onerror(error)
  {
    webRtcPeer.dispose()

    _onerror(error)
  }

  webRtcPeer.on('error', onerror)
  webRtcPeer.on('sdpoffer', function(sdpOffer)
  {
    var stream = this.getLocalStream()
    assert.notEqual(stream, undefined, 'local stream')

    var offer = new RTCSessionDescription(
    {
      type: 'offer',
      sdp:  sdpOffer
    });

    var peerConnection = new RTCPeerConnection()

    peerConnection.setRemoteDescription(offer, function()
    {
      var stream = peerConnection.getRemoteStreams()[0]
      assert.notEqual(stream, undefined, 'peer remote stream')

      peerConnection.createAnswer(function(answer)
      {
        peerConnection.setLocalDescription(answer, function()
        {
          webRtcPeer.processSdpAnswer(answer.sdp, function(error)
          {
            if(error) return onerror(error)

            this.dispose()

            done()
          })
        },
        onerror);
      },
      onerror);
    },
    onerror)
  })
});

QUnit.test('WebRtcPeerSendrecv', function(assert)
{
  var done = assert.async();

  assert.expect(3);

//  var localVideo  = document.getElementById('localVideo')
//  var remoteVideo = document.getElementById('remoteVideo')

  var webRtcPeer = new WebRtcPeerSendrecv()

  function onerror(error)
  {
    webRtcPeer.dispose()

    _onerror(error)
  }

  webRtcPeer.on('error', onerror)
  webRtcPeer.on('sdpoffer', function(sdpOffer)
  {
    var stream = this.getLocalStream()
    assert.notEqual(stream, undefined, 'local stream')

    var offer = new RTCSessionDescription(
    {
      type: 'offer',
      sdp:  sdpOffer
    });

    var peerConnection = new RTCPeerConnection()

    peerConnection.setRemoteDescription(offer, function()
    {
      var stream = peerConnection.getRemoteStreams()[0]
      assert.notEqual(stream, undefined, 'peer remote stream')

//      peerConnection.addStream(stream)

      getUserMedia(mediaConstraints, function(stream)
      {
        peerConnection.addStream(stream)

        peerConnection.createAnswer(function(answer)
        {
          peerConnection.setLocalDescription(answer, function()
          {
            webRtcPeer.processSdpAnswer(answer.sdp, function(error)
            {
              if(error) return onerror(error)

              var stream = this.getRemoteStream()
              assert.notEqual(stream, undefined, 'remote stream')

              this.dispose()
              done()
            })
          },
          onerror);
        },
        onerror);
      },
      onerror);
    },
    onerror)
  })
});
