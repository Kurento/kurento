/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.services;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_AUTOSTART_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_LOGIN_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_PASSWD_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_PEM_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_SCOPE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_SCOPE_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_WS_URI_DEFAULT;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_WS_URI_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_WS_URI_PROP_EXPORT;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_PROP;

import java.util.ArrayList;
import java.util.List;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.monitor.SystemMonitorManager;
import org.kurento.test.utils.WebRtcConnector;

/**
 * Fake Kurento Media Server service.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class FakeKmsService extends KmsService {

  protected List<WebRtcEndpoint> fakeWebRtcList = new ArrayList<>();
  protected List<WebRtcEndpoint> fakeBrowserList = new ArrayList<>();
  protected List<MediaPipeline> fakeMediaPipelineList = new ArrayList<>();

  public FakeKmsService() {
    this.kmsLoginProp = FAKE_KMS_LOGIN_PROP;
    this.kmsPasswdProp = FAKE_KMS_PASSWD_PROP;
    this.kmsPemProp = FAKE_KMS_PEM_PROP;
    this.kmsAutostartProp = FAKE_KMS_AUTOSTART_PROP;
    this.kmsAutostartDefault = FAKE_KMS_AUTOSTART_DEFAULT;
    this.kmsWsUriProp = FAKE_KMS_WS_URI_PROP;
    this.kmsWsUriExportProp = FAKE_KMS_WS_URI_PROP_EXPORT;
    this.kmsScopeProp = FAKE_KMS_SCOPE_PROP;
    this.kmsScopeDefault = FAKE_KMS_SCOPE_DEFAULT;

    // KMS_WS_URI_PROP has priority if there's no value for FAKE_KMS_WS_URI_PROP
    String uri = getProperty(FAKE_KMS_WS_URI_PROP);
    if (uri == null) {
      if (getProperty(KMS_WS_URI_PROP) != null) {
        this.kmsWsUriProp = KMS_WS_URI_PROP;
        setWsUri(getProperty(KMS_WS_URI_PROP));
      } else {
        setWsUri(FAKE_KMS_WS_URI_DEFAULT);
      }
    } else {
      setWsUri(uri);
    }
  }

  @Override
  protected String getDockerContainerNameSuffix() {
    return "_fakekms";
  }

  @Override
  protected String getDockerLogSuffix() {
    return "-fakekms";
  }

  public void addFakeClients(int numFakeClients, final int bandwidht,
      final MediaPipeline mainPipeline, final WebRtcEndpoint inputWebRtc, long timeBetweenClientMs,
      final SystemMonitorManager monitor, final WebRtcConnector connector) {

    if (kurentoClient == null) {
      throw new KurentoException("Fake kurentoClient for is not defined.");

    } else {
      log.debug("* * * Adding {} fake clients * * *", numFakeClients);
      final MediaPipeline fakePipeline = kurentoClient.createMediaPipeline();

      for (int i = 0; i < numFakeClients; i++) {

        log.debug("* * * Adding fake client {} * * *", i);

        new Thread() {
          @Override
          public void run() {

            final WebRtcEndpoint fakeOutputWebRtc =
                new WebRtcEndpoint.Builder(mainPipeline).build();
            final WebRtcEndpoint fakeBrowser = new WebRtcEndpoint.Builder(fakePipeline).build();

            if (bandwidht != -1) {
              fakeOutputWebRtc.setMaxVideoSendBandwidth(bandwidht);
              fakeOutputWebRtc.setMinVideoSendBandwidth(bandwidht);
              fakeBrowser.setMaxVideoRecvBandwidth(bandwidht);
            }

            fakeOutputWebRtc
                .addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                  @Override
                  public void onEvent(IceCandidateFoundEvent event) {
                    fakeBrowser.addIceCandidate(event.getCandidate());
                  }
                });

            fakeBrowser.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

              @Override
              public void onEvent(IceCandidateFoundEvent event) {
                fakeOutputWebRtc.addIceCandidate(event.getCandidate());
              }
            });

            String sdpOffer = fakeBrowser.generateOffer();
            String sdpAnswer = fakeOutputWebRtc.processOffer(sdpOffer);
            fakeBrowser.processAnswer(sdpAnswer);

            fakeOutputWebRtc.gatherCandidates();
            fakeBrowser.gatherCandidates();

            if (connector == null) {
              inputWebRtc.connect(fakeOutputWebRtc);
            } else {
              connector.connect(inputWebRtc, fakeOutputWebRtc);
            }

            fakeWebRtcList.add(fakeOutputWebRtc);
            fakeBrowserList.add(fakeBrowser);
            fakeMediaPipelineList.add(fakePipeline);
          }
        }.start();

        if (monitor != null) {
          monitor.incrementNumClients();
        }

        waitMs(timeBetweenClientMs);
      }
    }
  }

  public void releaseAllFakeClients(long timeBetweenClientMs, WebRtcEndpoint inputWebRtc,
      SystemMonitorManager monitor) {
    for (WebRtcEndpoint fakeWebRtc : fakeWebRtcList) {
      fakeWebRtc.release();
      monitor.decrementNumClients();

      waitMs(timeBetweenClientMs);
    }
    for (WebRtcEndpoint fakeBrowser : fakeBrowserList) {
      fakeBrowser.release();
      waitMs(timeBetweenClientMs);
    }
    fakeWebRtcList = new ArrayList<>();
    fakeBrowserList = new ArrayList<>();
  }

  public void releaseAllFakePipelines(long timeBetweenClientMs, SystemMonitorManager monitor) {
    for (int i = 0; i < fakeWebRtcList.size(); i++) {
      monitor.decrementNumClients();
      waitMs(timeBetweenClientMs);
    }
    for (WebRtcEndpoint fakeBrowser : fakeBrowserList) {
      fakeBrowser.release();
      waitMs(timeBetweenClientMs);
    }
    for (MediaPipeline fakeMediaPipeline : fakeMediaPipelineList) {
      fakeMediaPipeline.release();
    }
    fakeWebRtcList = new ArrayList<>();
    fakeBrowserList = new ArrayList<>();
    fakeMediaPipelineList = new ArrayList<MediaPipeline>();
  }

  public void removeAllFakeClients(long timeBetweenClientMs, WebRtcEndpoint inputWebRtc,
      SystemMonitorManager monitor) {
    for (WebRtcEndpoint fakeWebRtc : fakeWebRtcList) {
      inputWebRtc.disconnect(fakeWebRtc);
      monitor.decrementNumClients();

      waitMs(timeBetweenClientMs);
    }
  }

  private void waitMs(long timeBetweenClientMs) {
    if (timeBetweenClientMs > 0) {
      try {
        Thread.sleep(timeBetweenClientMs);
      } catch (InterruptedException e) {
        log.warn("Interrupted exception working with fake clients", e);
      }
    }
  }

}
