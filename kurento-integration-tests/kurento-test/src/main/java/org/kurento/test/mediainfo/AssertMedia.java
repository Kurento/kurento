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

package org.kurento.test.mediainfo;

import java.io.File;

import org.junit.Assert;

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
    MediaInfo info = new MediaInfo();
    info.open(new File(pathToMedia));
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
    MediaInfo info = new MediaInfo();
    info.open(new File(pathToMedia));
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
    MediaInfo info = new MediaInfo();
    info.open(new File(pathToMedia));
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

}
