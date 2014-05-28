/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

var handlerUrl = 'https://raw.github.com/Kurento/kmf-content-demo/develop/src/main/webapp';

var Windows =
{
  DK:
  {
    id:            'DK',
    height:        100,
    width:         100,
    upperRightX:   540,
    upperRightY:   126,
    image:         handlerUrl+'/img/buttons/dk.png',
    inactiveImage: handlerUrl+'/img/buttons/dk.png'
  },

  FIWARE:
  {
    id:            'FIWARE',
    height:        40,
    width:         180,
    upperRightX:   230,
    upperRightY:   0,
    image:         handlerUrl+'/img/buttons/fiware2.png',
    inactiveImage: handlerUrl+'/img/buttons/fiware2.png'
  },

  MARIO:
  {
    id:            'MARIO',
    height:        100,
    width:         100,
    upperRightX:   540,
    upperRightY:   0,
    image:         handlerUrl+'/img/buttons/mario.png',
    inactiveImage: handlerUrl+'/img/buttons/mario.png'
  },

  SF:
  {
    id:            'SF',
    height:        100,
    width:         100,
    upperRightX:   540,
    upperRightY:   252,
    image:         handlerUrl+'/img/buttons/sf.png',
    inactiveImage: handlerUrl+'/img/buttons/sf.png'
  },

  SONIC:
  {
    id:            'SONIC',
    height:        100,
    width:         100,
    upperRightX:   540,
    upperRightY:   380,
    image:         handlerUrl+'/img/buttons/sonic.png',
    inactiveImage: handlerUrl+'/img/buttons/sonic.png'
  },

  START:
  {
    id:            'START',
    height:        100,
    width:         100,
    upperRightX:   280,
    upperRightY:   380,
    image:         handlerUrl+'/img/buttons/start.png',
    inactiveImage: handlerUrl+'/img/buttons/start.png'
  },

  TRASH:
  {
    id:            'TRASH',
    height:        100,
    width:         100,
    upperRightX:   0,
    upperRightY:   100,
    image:         handlerUrl+'/img/buttons/trash.png',
    inactiveImage: handlerUrl+'/img/buttons/trash.png'
  },

  YOUTUBE:
  {
    id:            'YOUTUBE',
    height:        100,
    width:         100,
    upperRightX:   380,
    upperRightY:   0,
    image:         handlerUrl+'/img/buttons/youtube.png',
    inactiveImage: handlerUrl+'/img/buttons/youtube.png'
  }
};

var PLAYLIST_TOKEN = "PL58tWS2XjtialwG-eWDYoFwQpHTd5vDEE";


function CpbWebRtc()
{
  var activeWindow = Windows.START;

  var count = 0;
  var mario = 0;

  var pointerDetectorAdvFilter = null;
  var chromaFilter             = null;
  var faceOverlayFilter        = null;
  var recorderEndpoint         = null;


  function checkEasterEgg()
  {
    count++;

    if(count % 20 == 0)
    {
      // Each 20 times (20, 40, ...) Darth Vader hat/background is shown
      setStarWars();

      return true;
    };

    if(count % 10 == 0)
    {
      // Each 10 times (10, 30, ...) the Jack Sparrow hat/background is shown
      setPirates();

      return true;
    };

    return false;
  };

  function checkFirstTime()
  {
    if(activeWindow === Windows.START)
    {
      pointerDetectorAdvFilter.addWindow(cpbWindows.trash);
      pointerDetectorAdvFilter.addWindow(cpbWindows.youtube);
    };
  };


  function setDK()
  {
    if(activeWindow === Windows.DK || checkEasterEggs()) return;

    chromaFilter.setBackground(handlerUrl + "/img/background/dk.jpg");
    faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/dk.png",
                                        -0.35, -0.5, 1.6, 1.6);

    checkFirstTime();
    activeWindow = Windows.DK;
  };

  function setMario()
  {
    if(activeWindow === Windows.MARIO || checkEasterEggs()) return;

    chromaFilter.setBackground(handlerUrl + "/img/background/mario.jpg");

    mario++;

    // Mario Easter Egg (a different mask each time)
    if(mario % 2 == 0)
      faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/mario-wings.png",
                                          -0.35, -1.2, 1.6, 1.6);
    else
      faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/mario.png",
                                          -0.3, -0.6, 1.6, 1.6);

    checkFirstTime();
    activeWindow = Windows.MARIO;
  };

  function setPirates()
  {
    chromaFilter.setBackground(handlerUrl + "/img/background/pirates.jpg");
    faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/jack.png",
                                        -0.4, -0.4, 1.7, 1.7);
  };

  function setSF()
  {
    if(activeWindow === Windows.SF || checkEasterEggs()) return;

    chromaFilter.setBackground(handlerUrl + "/img/background/sf.jpg");
    faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/sf.png",
                                        -0.35, -0.5, 1.6, 1.6);

    checkFirstTime();
    activeWindow = Windows.SF;
  };

  function setSonic()
  {
    if(activeWindow !== Windows.SONIC && !checkEasterEggs())
    {
      chromaFilter.setBackground(handlerUrl + "/img/background/sonic.jpg");
      faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/sonic.png",
                                          -0.5, -0.5, 1.7, 1.7);

      checkFirstTime();
      activeWindow = Windows.SONIC;
    };
  };

  function setStarWars()
  {
    chromaFilter.setBackground(handlerUrl + "/img/background/deathstar.jpg");
    faceOverlayFilter.setOverlayedImage(handlerUrl + "/img/masks/darthvader.png",
                                        -0.5, -0.5, 1.7, 1.7);
  };


  function setStart()
  {
    RecorderEndpoint.create(pipeline, function(error, endpoint)
    {
      if(error) return console.error(error);

      recorderEndpoint = endpoint;

      faceOverlayFilter.connect(recorderEndpoint);

      pointerDetectorAdvFilter.clearWindows();

      pointerDetectorAdvFilter.addWindow(Windows.DK);
      pointerDetectorAdvFilter.addWindow(Windows.FIWARE);
      pointerDetectorAdvFilter.addWindow(Windows.MARIO);
      pointerDetectorAdvFilter.addWindow(Windows.SF);
      pointerDetectorAdvFilter.addWindow(Windows.SONIC);

      recorderEndpoint.record();
    });
  };

  function setEnding(windowID)
  {
    chromaFilter.unsetBackground();

    pointerDetectorAdvFilter.clearWindows();
    pointerDetectorAdvFilter.addWindow(Windows.FIWARE);
    pointerDetectorAdvFilter.addWindow(Windows.START);

    faceOverlayFilter.unsetOverlayedImage();

    activeWindow = Windows.START;

    recorderEndpoint.release();

    if(windowID == Windows.YOUTUBE.id)
    {
      console.info('recordUrl ');


    };
  };


  function onWindowIn(windowID)
  {
    switch(windowID)
    {
      case 'DK':     setDK();     break;

      case 'FIWARE':              break;
      case 'MARIO':  setMario();  break;

      case 'SF':     setSF();     break;
      case 'SONIC':  setSonic();  break;
      case 'START':  setStart();  break;

      case 'TRASH':
      case 'YOUTUBE':  setEnding(windowID);  break;

      default:
        console.warning('Unknown windowID',windowID);
    };
  };


  // Create a new connection
  var contentId = document.getElementById("contentId");
      contentId = contentId.disabled ? "" : "/" + contentId.value;

  var endpoint = 'ws://kms01.kurento.org:8080/thrift/ws/websocket';
//  var endpoint = 'ws://kms01.kurento.org:8080/thrift/ws/websocket' + contentId;

  var kwsMedia = null;


  /**
   * Javascript port of the Campus Party Brazil 2014 Kurento demo
   *
   * This demo has the following pipeline:
   *
   * WebRTC -> RateLimiter -> MirrorFilter -> PointerDetectorFilter ->
   * ChromaFilter -> FaceOverlayFilter -> Recorder
   *
   * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
   * @since 1.0.0
   */
  this.start = function(offer, callback)
  {
    kwsMedia = KwsMedia(endpoint, function(kwsMedia)
    {
      // Create pipeline
      kwsMedia.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return callback(error);

        pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
        {
          if(error) return callback(error);

          // Create pipeline media elements (endpoints & filters)
          pipeline.create('RateFilter', function(error, rateFilter)
          {
            if(error) return callback(error);

            webRtcEndpoint.connect(rateFilter, function(error)
            {
              if(error) return callback(error);

              pipeline.create('MirrorFilter', function(error, mirrorFilter)
              {
                if(error) return callback(error);

                rateFilter.connect(mirrorFilter, function(error)
                {
                  if(error) return callback(error);

                  var calibrationRegion =
                  {
                    topRightCornerX: 5,
                    topRightCornerY: 5,
                    width: 50,
                    height: 50
                  };
                  var params = {calibrationRegion: calibrationRegion};

                  pipeline.create('PointerDetectorAdvFilter', params,
                  function(error, filter)
                  {
                    if(error) return callback(error);

                    pointerDetectorAdvFilter = filter;

                    pointerDetectorAdvFilter.addWindow(Windows.START,
                    function(error)
                    {
                      if(error) return callback(error);

                      pointerDetectorAdvFilter.addWindow(Windows.FIWARE,
                      function(error)
                      {
                        if(error) return callback(error);

                        pointerDetectorAdvFilter.on('WindowIn', onWindowIn);

                        mirrorFilter.connect(pointerDetectorAdvFilter,
                        function(error)
                        {
                          if(error) return callback(error);

                          var window =
                          {
                            topRightCornerX: 100,
                            topRightCornerY: 10,
                            width: 500,
                            height: 400
                          };

                          pipeline.create('ChromaFilter', {window: window},
                          function(error, filter)
                          {
                            if(error) return callback(error);

                            chromaFilter = filter;

                            pointerDetectorAdvFilter.connect(chromaFilter,
                            function(error)
                            {
                              if(error) return callback(error);

                              pipeline.create('FaceOverlayFilter',
                              function(error, filter)
                              {
                                if(error) return callback(error);

                                faceOverlayFilter = filter;

                                chromaFilter.connect(faceOverlayFilter,
                                function(error)
                                {
                                  if(error) return callback(error);

                                  // Loopback
                                  faceOverlayFilter.connect(webRtcEndpoint,
                                  function(error)
                                  {
                                    if(error) return callback(error);

                                    webRtcEndpoint.processOffer(offer.sdp, callback);
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
              });
            });
          });
        });
      });

      kwsMedia.on("error", callback);
    },
    callback);
  };

  this.terminate = function()
  {
    if(kwsMedia)
    {
      kwsMedia.close();
      kwsMedia = null;
    }
    else
      console.warn('kwsMedia is not initialized');
  };

  this.calibrate = function()
  {
    if(pointerDetectorAdvFilter)
       pointerDetectorAdvFilter.trackcolourFromCalibrationRegion(function(error)
       {
         if(error) console.error(error);
       });
    else
      console.warn('pointerDetectorAdvFilter is not initialized');
  };
};
