/*
 * (C) Copyright 2015-2016 Kurento (http://kurento.org/)
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
package org.kurento.tutorial.helloworld;

import java.util.Date;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.springframework.web.socket.WebSocketSession;

/**
 * User session.
 *
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.1.1
 */
public class UserSession {
  private String id;
  private WebRtcEndpoint webRtcEndpoint;
  private MediaPipeline mediaPipeline;
  private RepositoryItemRecorder repoItem;
  private Date stopTimestamp;

  public UserSession(WebSocketSession session) {
    this.id = session.getId();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WebRtcEndpoint getWebRtcEndpoint() {
    return webRtcEndpoint;
  }

  public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
    this.webRtcEndpoint = webRtcEndpoint;
  }

  public MediaPipeline getMediaPipeline() {
    return mediaPipeline;
  }

  public void setMediaPipeline(MediaPipeline mediaPipeline) {
    this.mediaPipeline = mediaPipeline;
  }

  public RepositoryItemRecorder getRepoItem() {
    return repoItem;
  }

  public void setRepoItem(RepositoryItemRecorder repoItem) {
    this.repoItem = repoItem;
  }

  public void addCandidate(IceCandidate candidate) {
    webRtcEndpoint.addIceCandidate(candidate);
  }

  public Date getStopTimestamp() {
    return stopTimestamp;
  }

  public void release() {
    this.mediaPipeline.release();
    this.webRtcEndpoint = null;
    this.mediaPipeline = null;
    if (this.stopTimestamp == null) {
      this.stopTimestamp = new Date();
    }
  }
}
