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

package org.kurento.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.module.crowddetector.CrowdDetectorDirectionEvent;
import org.kurento.module.crowddetector.CrowdDetectorFilter;
import org.kurento.module.crowddetector.CrowdDetectorFluidityEvent;
import org.kurento.module.crowddetector.CrowdDetectorOccupancyEvent;
import org.kurento.module.crowddetector.RegionOfInterest;
import org.kurento.module.crowddetector.RegionOfInterestConfig;
import org.kurento.module.crowddetector.RelativePoint;
import org.kurento.orion.OrionConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Pipeline {

  private static final Logger log = LoggerFactory.getLogger(Pipeline.class);
  private static final long RECONNECTION_TIME = 30 * 1000;

  @Autowired
  private ConfigurationReader configuration;

  @Autowired
  private KurentoClient kurento;

  @Autowired
  private CrowdDetectorOrionPublisher orionPublisher;

  private MediaPipeline pipe;
  private PlayerEndpoint playerEndpoint;
  private CrowdDetectorFilter crowdDetectorFilter;
  private String feedUrl;
  private List<RegionOfInterest> rois;
  private final Map<String, WebRtcEndpoint> webRtcEndpoints = new ConcurrentHashMap<>();
  private boolean playing;
  private Timer timer;

  @PostConstruct
  public void init() {

    this.pipe = this.kurento.createMediaPipeline();

    this.rois = new ArrayList<>();
    JsonArray loadedRois;

    JsonObject config = this.configuration.getConfig();

    if (config != null) {
      JsonElement feedUrlJson = config.get("feedUrl");
      if (feedUrlJson != null) {
        this.feedUrl = feedUrlJson.getAsString();
      } else {
        log.debug("Url feed not defined.");
      }

      JsonElement loadedRoisJson = config.get("rois");
      if (loadedRoisJson != null) {
        loadedRois = loadedRoisJson.getAsJsonArray();
        this.rois = readRoisFromJson(loadedRois);
      } else {
        this.rois = getDummyRois();
        log.debug("Rois not defined. Using dummy rois");
      }
    }

    if (this.feedUrl == null) {
      // PlayerEndpoint will be configured later.
      return;
    }

    try {
      this.orionPublisher.registerRoisInOrion(this.rois);
    } catch (OrionConnectorException e) {
      log.warn("Could not register ROIs in ORION");
    }

    this.crowdDetectorFilter = new CrowdDetectorFilter.Builder(this.pipe, this.rois).build();
    this.crowdDetectorFilter.setProcessingWidth(640);

    addOrionListeners();

    this.playerEndpoint = new PlayerEndpoint.Builder(this.pipe, this.feedUrl).build();
    addPlayerListeners();
    this.playing = true;

    this.playerEndpoint.connect(this.crowdDetectorFilter);
    this.playerEndpoint.play();
  }

  private void addPlayerListeners() {
    this.playerEndpoint.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
      @Override
      public void onEvent(EndOfStreamEvent event) {
        log.warn("Received EOS from Player");
        if (Pipeline.this.timer != null) {
          try {
            Pipeline.this.timer.cancel();
          } catch (Exception e) {
            log.debug("Empty timer");
          }
        }
        setFeedUrl(Pipeline.this.feedUrl);
      }
    });

    this.playerEndpoint.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent event) {
        synchronized (this) {
          log.error("Error received from Media server: {} (code {}, type {})",
              event.getDescription(), event.getErrorCode(), event.getType());
          if (Pipeline.this.playing) {
            Pipeline.this.playing = false;
            log.error("Timer added to create the player again");
            Pipeline.this.timer = new Timer();

            TimerTask task = new TimerTask() {
              @Override
              public void run() {
                setFeedUrl(Pipeline.this.feedUrl);
              }
            };
            Pipeline.this.timer.schedule(task, RECONNECTION_TIME);
          }
        }
      }
    });
  }

  private static List<RegionOfInterest> readRoisFromJson(JsonArray loadedRois) {

    List<RegionOfInterest> rois = new ArrayList<>();

    for (int j = 0; j < loadedRois.size(); j++) {

      JsonObject roi = (JsonObject) loadedRois.get(j);

      JsonArray coordinates = (JsonArray) roi.get("points");
      // create structure to configure crowddetector
      List<RelativePoint> points = new ArrayList<>(coordinates.size());
      for (int i = 0; i < coordinates.size(); i++) {
        JsonObject coordinate = coordinates.get(i).getAsJsonObject();

        float x = coordinate.getAsJsonPrimitive("x").getAsFloat();
        float y = coordinate.getAsJsonPrimitive("y").getAsFloat();

        if (x > 1) {
          x = 1;
        }

        if (y > 1) {
          y = 1;
        }

        points.add(new RelativePoint(x, y));
      }

      RegionOfInterestConfig config = new RegionOfInterestConfig();

      config.setFluidityLevelMin(roi.getAsJsonPrimitive("fluidityLevelMin").getAsInt());
      config.setFluidityLevelMed(roi.getAsJsonPrimitive("fluidityLevelMed").getAsInt());
      config.setFluidityLevelMax(roi.getAsJsonPrimitive("fluidityLevelMax").getAsInt());
      config.setFluidityNumFramesToEvent(
          roi.getAsJsonPrimitive("fluidityNumFramesToEvent").getAsInt());
      config.setOccupancyLevelMin(roi.getAsJsonPrimitive("occupancyLevelMin").getAsInt());
      config.setOccupancyLevelMed(roi.getAsJsonPrimitive("occupancyLevelMed").getAsInt());
      config.setOccupancyLevelMax(roi.getAsJsonPrimitive("occupancyLevelMax").getAsInt());
      config.setOccupancyNumFramesToEvent(
          roi.getAsJsonPrimitive("occupancyNumFramesToEvent").getAsInt());

      config.setSendOpticalFlowEvent(roi.getAsJsonPrimitive("sendOpticalFlowEvent").getAsBoolean());

      config.setOpticalFlowNumFramesToEvent(
          roi.getAsJsonPrimitive("opticalFlowNumFramesToEvent").getAsInt());
      config.setOpticalFlowNumFramesToReset(
          roi.getAsJsonPrimitive("opticalFlowNumFramesToReset").getAsInt());
      config.setOpticalFlowAngleOffset(roi.getAsJsonPrimitive("opticalFlowAngleOffset").getAsInt());

      rois.add(new RegionOfInterest(points, config, roi.getAsJsonPrimitive("id").getAsString()));
    }

    return rois;
  }

  private static List<RegionOfInterest> getDummyRois() {

    List<RelativePoint> points = new ArrayList<>();

    float x = 0;
    float y = 0;
    points.add(new RelativePoint(x, y));

    x = 1;
    y = 0;
    points.add(new RelativePoint(x, y));

    x = 1;
    y = 1;
    points.add(new RelativePoint(x, y));

    x = 0;
    y = 1;
    points.add(new RelativePoint(x, y));

    RegionOfInterestConfig config = new RegionOfInterestConfig();

    config.setFluidityLevelMin(10);
    config.setFluidityLevelMed(35);
    config.setFluidityLevelMax(65);
    config.setFluidityNumFramesToEvent(5);
    config.setOccupancyLevelMin(10);
    config.setOccupancyLevelMed(35);
    config.setOccupancyLevelMax(65);
    config.setOccupancyNumFramesToEvent(5);

    config.setSendOpticalFlowEvent(false);

    config.setOpticalFlowNumFramesToEvent(3);
    config.setOpticalFlowNumFramesToReset(3);
    config.setOpticalFlowAngleOffset(0);

    List<RegionOfInterest> rois = new ArrayList<>();
    rois.add(new RegionOfInterest(points, config, "dummyRoy"));

    return rois;
  }

  public void addOrionListeners() {

    this.crowdDetectorFilter
        .addCrowdDetectorDirectionListener(new EventListener<CrowdDetectorDirectionEvent>() {
          @Override
          public void onEvent(CrowdDetectorDirectionEvent event) {

            try {
              Pipeline.this.orionPublisher.publishEvent(event);
            } catch (OrionConnectorException e) {
              log.warn("Could not publish event in ORION");
            }

            log.debug("Direction event detected in roi {} direction {}", event.getRoiID(),
                event.getDirectionAngle());
          }
        });

    this.crowdDetectorFilter
        .addCrowdDetectorFluidityListener(new EventListener<CrowdDetectorFluidityEvent>() {
          @Override
          public void onEvent(CrowdDetectorFluidityEvent event) {

            try {
              Pipeline.this.orionPublisher.publishEvent(event);
            } catch (OrionConnectorException e) {
              log.warn("Could not publish event in ORION");
            }

            log.debug("Fluidity event detected in roi {} percentage {}  level {}", event.getRoiID(),
                event.getFluidityPercentage(), event.getFluidityLevel());
          }
        });

    this.crowdDetectorFilter
        .addCrowdDetectorOccupancyListener(new EventListener<CrowdDetectorOccupancyEvent>() {
          @Override
          public void onEvent(CrowdDetectorOccupancyEvent event) {

            try {
              Pipeline.this.orionPublisher.publishEvent(event);
            } catch (OrionConnectorException e) {
              log.warn("Could not publish event in ORION");
            }

            log.debug("Occupancy event detected in roi {} percentage {} level {}", event.getRoiID(),
                event.getOccupancyPercentage(), event.getOccupancyLevel());
          }
        });

  }

  public CrowdDetectorFilter getCrowdDetectorFilter() {
    return this.crowdDetectorFilter;
  }

  public PlayerEndpoint getPlayerEndpoint() {
    return this.playerEndpoint;
  }

  public String getFeedUrl() {
    return this.feedUrl;
  }

  public void setFeedUrl(String feedUrl) {
    this.feedUrl = feedUrl;

    if (this.playerEndpoint != null) {
      log.debug("Releasing previous elements");
      this.crowdDetectorFilter.release();
      this.playerEndpoint.release();
    }

    log.debug("Creating new elements");

    this.crowdDetectorFilter = new CrowdDetectorFilter.Builder(this.pipe, this.rois).build();
    this.crowdDetectorFilter.setProcessingWidth(640);

    addOrionListeners();

    this.playerEndpoint = new PlayerEndpoint.Builder(this.pipe, this.feedUrl).build();
    addPlayerListeners();
    this.playing = true;

    this.playerEndpoint.connect(this.crowdDetectorFilter);
    this.playerEndpoint.play();

    log.debug("New player is now runing");
    log.debug("Connecting " + this.webRtcEndpoints.size() + " webrtcendpoints");
    // change the feed for all the webrtc clients connected.
    for (Entry<String, WebRtcEndpoint> ep : this.webRtcEndpoints.entrySet()) {
      this.crowdDetectorFilter.connect(ep.getValue());
    }
  }

  public MediaPipeline getPipeline() {
    return this.pipe;
  }

  public List<RegionOfInterest> getRois() {
    return this.rois;
  }

  public void setWebRtcEndpoint(String session, WebRtcEndpoint webRtcEndpoint) {
    this.webRtcEndpoints.put(session, webRtcEndpoint);
  }

  public void removeWebRtcEndpoint(String session) {
    if (this.webRtcEndpoints.containsKey(session)) {
      this.webRtcEndpoints.get(session).release();
      this.webRtcEndpoints.remove(session);
      log.debug("client removed");
    }
  }

  public boolean isPlaying() {
    return this.playing;
  }

  public void addCandidate(IceCandidate candidate, String session) {
    WebRtcEndpoint endpoint = this.webRtcEndpoints.get(session);
    if (endpoint != null) {
      endpoint.addIceCandidate(candidate);
    }
  }
}
