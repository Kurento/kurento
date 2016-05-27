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

package org.kurento.test.browser;

/**
 * Type of channel in WebRTC communications (audio, video, or both).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public enum WebRtcChannel {
  VIDEO_ONLY, AUDIO_ONLY, AUDIO_AND_VIDEO;

  public String getJsFunction() {
    switch (this) {
      case VIDEO_ONLY:
        return "setVideoUserMediaConstraints();";
      case AUDIO_ONLY:
        return "setAudioUserMediaConstraints()";
      case AUDIO_AND_VIDEO:
        // Audio and video is the default options in kurento-utils.js, so
        // user media constrains should not be changed in this case
        return null;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    switch (this) {
      case VIDEO_ONLY:
        return "(VIDEO ONLY)";
      case AUDIO_ONLY:
        return "(AUDIO ONLY)";
      case AUDIO_AND_VIDEO:
        return "(VIDEO & AUDIO)";
      default:
        throw new IllegalArgumentException();
    }
  }

}
