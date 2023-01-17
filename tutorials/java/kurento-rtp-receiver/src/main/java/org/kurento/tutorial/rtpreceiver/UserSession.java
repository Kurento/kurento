/*
 * Copyright 2018 Kurento (https://www.kurento.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.tutorial.rtpreceiver;

import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.WebRtcEndpoint;

public class UserSession
{
  private MediaPipeline mediaPipeline;
  private RtpEndpoint rtpEp;
  private WebRtcEndpoint webRtcEp;

  public UserSession()
  {}

  public MediaPipeline getMediaPipeline()
  {
    return mediaPipeline;
  }

  public void setMediaPipeline(MediaPipeline mediaPipeline)
  {
    this.mediaPipeline = mediaPipeline;
  }

  public RtpEndpoint getRtpEndpoint()
  {
    return rtpEp;
  }

  public void setRtpEndpoint(RtpEndpoint rtpEp)
  {
    this.rtpEp = rtpEp;
  }

  public WebRtcEndpoint getWebRtcEndpoint()
  {
    return webRtcEp;
  }

  public void setWebRtcEndpoint(WebRtcEndpoint webRtcEp)
  {
    this.webRtcEp = webRtcEp;
  }
}
