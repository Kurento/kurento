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

package org.kurento.test.latency;

import java.awt.Color;

/**
 * Events for color change detection.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ChangeColorEvent {

  private VideoTag videoTag;
  private long time;
  private Color color;

  public ChangeColorEvent(VideoTag videoTag, long time, Color color) {
    this.videoTag = videoTag;
    this.time = time;
    this.color = color;
  }

  public VideoTag getVideoTag() {
    return videoTag;
  }

  public long getTime() {
    return time;
  }

  public void setVideoTag(VideoTag videoTag) {
    this.videoTag = videoTag;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

}
