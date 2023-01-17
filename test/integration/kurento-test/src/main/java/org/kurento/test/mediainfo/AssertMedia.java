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

package org.kurento.test.mediainfo;

import java.io.File;

import org.junit.Assert;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.config.Protocol;
import org.kurento.test.utils.Shell;

import com.google.common.base.Strings;

/**
 * Utility class to assert the expected codecs in a media (video/audio) file.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.3.1
 */
public class AssertMedia {

  public static void assertCodecs(String pathToMedia, String expectedVideoCodec,
      String expectedAudioCodec) {
    MediaInfo info = getInfoByPath(pathToMedia);
    String videoFormat = info.get(MediaInfo.StreamKind.Video, 0, "Format", MediaInfo.InfoKind.Text,
        MediaInfo.InfoKind.Name);
    String audioFormat = info.get(MediaInfo.StreamKind.Audio, 0, "Format", MediaInfo.InfoKind.Text,
        MediaInfo.InfoKind.Name);
    info.close();

    if (expectedVideoCodec != null) {
      Assert.assertEquals("Wrong video codec in " + pathToMedia, expectedVideoCodec, videoFormat);
    }
    if (expectedAudioCodec != null) {
      Assert.assertEquals("Wrong audio codec in " + pathToMedia, expectedAudioCodec, audioFormat);
    }
  }

  public static void assertDuration(String pathToMedia, double expectedDurationMs,
      double thresholdMs) {
    assertAudioDuration(pathToMedia, expectedDurationMs, thresholdMs);
    assertGeneralDuration(pathToMedia, expectedDurationMs, thresholdMs);
  }

  public static void assertAudioDuration(String pathToMedia, double expectedDurationMs,
      double thresholdMs) {
    MediaInfo info = getInfoByPath(pathToMedia);
    String audioDuration = info.get(MediaInfo.StreamKind.Audio, 0, "Duration",
        MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
    info.close();

    Assert.assertFalse("Audio duration is empty or null in " + pathToMedia,
        Strings.isNullOrEmpty(audioDuration));

    long audioDurationMs = Long.parseLong(audioDuration);
    double difference = Math.abs(audioDurationMs - expectedDurationMs);

    Assert.assertTrue("Wrong audio duration (expected=" + expectedDurationMs + " ms, real= "
        + audioDurationMs + " ms) in " + pathToMedia, difference < thresholdMs);
  }

  public static void assertGeneralDuration(String pathToMedia, double expectedDurationMs,
      double thresholdMs) {
    MediaInfo info = getInfoByPath(pathToMedia);
    String generalDuration = info.get(MediaInfo.StreamKind.General, 0, "Duration",
        MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
    info.close();

    Assert.assertFalse("General duration is empty or null in " + pathToMedia,
        Strings.isNullOrEmpty(generalDuration));

    long generalDurationMs = Long.parseLong(generalDuration);
    double difference = Math.abs(generalDurationMs - expectedDurationMs);

    Assert.assertTrue("Wrong general duration (expected=" + expectedDurationMs + " ms, real= "
        + generalDurationMs + " ms) in " + pathToMedia, difference < thresholdMs);
  }

  private static MediaInfo getInfoByPath(String pathToMedia) {
    MediaInfo info = new MediaInfo();
    String pathToMedia_[] = pathToMedia.split("://");

    String protocol = "";
    String path = "";

    if (pathToMedia_.length > 1) {
      protocol = pathToMedia_[0];
      path = pathToMedia_[1];
    } else {
      String recordDefaultPath = KurentoTest.getRecordDefaultPath();
      if (recordDefaultPath != null) {
        String defaultPathToMedia_[] = recordDefaultPath.split("://");
        protocol = defaultPathToMedia_[0];
        String pathStart = defaultPathToMedia_[1];

        path = pathStart + pathToMedia_[0];
      }
    }

    if (Protocol.FILE.toString().equals(protocol)) {
      info.open(new File(path));
      return info;
    } else if (Protocol.HTTP.toString().equals(protocol)
        || Protocol.HTTPS.toString().equals(protocol)) {
      // TODO Get uri from client repository and use wget
    } else if (Protocol.S3.toString().equals(protocol)) {
      pathToMedia = protocol + "://" + path;
      String pathDownload =
          KurentoTest.getDefaultOutputFolder().getAbsolutePath() + File.separator + path;
      String pathOut = KurentoTest.getDefaultOutputFolder().getAbsolutePath() + File.separator
          + path.replace("/test", "/ffmpeg");
      // Download file from S3
      Shell.runAndWaitString("aws s3 cp " + pathToMedia + " " + pathDownload);
      // Use ffmpeg for adding duration
      Shell.runAndWaitString(
          "ffmpeg -y -i " + pathDownload + " -c:a copy -c:v copy -map 0 " + pathOut);
      info.open(new File(pathOut));
      return info;
    } else if (Protocol.MONGODB.toString().equals(protocol)) {
      // TODO
    }

    return info;
  }
}
