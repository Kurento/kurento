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

/**
 * Video tag for color detection (used in latency control).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class VideoTag {

  private String color;
  private String time;
  private String name;
  private VideoTagType videoTagType;

  public VideoTag(VideoTagType videoTagType, String mapKey) {
    this.videoTagType = videoTagType;
    this.color = "return kurentoTest.colorInfo['" + mapKey + "'].changeColor;";
    this.time = "return kurentoTest.colorInfo['" + mapKey + "'].changeTime;";
    this.name = mapKey;
  }

  public VideoTag(VideoTagType videoTagType) {
    this.videoTagType = videoTagType;
    this.color = videoTagType.getColor();
    this.time = videoTagType.getTime();
    this.name = videoTagType.getName();
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public VideoTagType getVideoTagType() {
    return videoTagType;
  }

  public void setVideoTagType(VideoTagType videoTagType) {
    this.videoTagType = videoTagType;
  }

}
