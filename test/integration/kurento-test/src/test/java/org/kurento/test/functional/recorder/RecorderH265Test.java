/*
 * (C) Copyright 2026 Kurento (https://kurento.openvidu.io/)
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

package org.kurento.test.functional.recorder;

import static org.kurento.client.MediaProfileSpecType.MKV_VIDEO_ONLY;
import static org.kurento.client.MediaProfileSpecType.MP4_VIDEO_ONLY;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.MediaTranscodingState;
import org.kurento.client.MediaTranscodingStateChangedEvent;
import org.kurento.client.MediaType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.RecorderVideoCodec;
import org.kurento.client.StoppedEvent;
import org.kurento.commons.testing.SystemFunctionalTests;
import org.kurento.test.base.KurentoClientTest;
import org.kurento.test.mediainfo.AssertMedia;
import org.kurento.test.utils.Shell;

/**
 * Test of a RecorderEndpoint that stores H.265 video, with the <code>videoCodec</code> constructor
 * parameter, into MP4 and MKV files.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint (H.265 file, encoded media) -> RecorderEndpoint (MP4 | MKV, H265)</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>None</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(Test) An H.265 file is generated with GStreamer (<code>x265enc</code>) or FFmpeg
 * (<code>libx265</code>). The test is skipped if neither is available, or if KMS runs in a
 * different host and cannot read the file.</li>
 * <li>(KMS) The file is played with <code>useEncodedMedia</code> and recorded with
 * <code>videoCodec=H265</code>.</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>No <code>MediaTranscodingStateChanged</code> event with state TRANSCODING is received: the
 * video is stored as-is (passthrough)</li>
 * <li>The recording contains H.265 video (<code>hevc</code>), and the MP4 file uses the
 * <code>hvc1</code> tag</li>
 * <li>The duration is as expected</li>
 * </ul>
 *
 * @since 7.4.0
 */
@Category(SystemFunctionalTests.class)
public class RecorderH265Test extends KurentoClientTest {

  private static final int FILE_SECONDS = 5;
  private static final int THRESHOLD_MS = 2000;

  @Test
  public void testRecorderH265Mp4() throws Exception {
    doTest(MP4_VIDEO_ONLY, ".mp4", "hvc1");
  }

  @Test
  public void testRecorderH265Mkv() throws Exception {
    // The MKV container describes the codec with a "hvcC" record, there is no MP4-like tag
    doTest(MKV_VIDEO_ONLY, ".mkv", null);
  }

  /**
   * Generates a short H.265 MP4 file with a test pattern; returns false if there is no encoder.
   */
  private boolean generateSource(File source) {
    String[] gst = {"gst-launch-1.0", "-q", "videotestsrc", "num-buffers=" + (FILE_SECONDS * 25),
        "!", "video/x-raw,width=320,height=240,framerate=25/1", "!", "x265enc", "!", "h265parse",
        "!", "mp4mux", "!", "filesink", "location=" + source.getAbsolutePath()};
    Shell.runAndWaitArray(gst);
    if (source.length() > 0) {
      return true;
    }

    String[] ffmpeg = {"ffmpeg", "-y", "-f", "lavfi", "-i",
        "testsrc=size=320x240:rate=25:duration=" + FILE_SECONDS, "-c:v", "libx265", "-tag:v",
        "hvc1", source.getAbsolutePath()};
    Shell.runAndWaitArray(ffmpeg);
    return source.length() > 0;
  }

  private String probe(String file, String entries) {
    return Shell.runAndWaitArray(new String[] {"ffprobe", "-v", "error", "-select_streams", "v:0",
        "-show_entries", entries, "-of", "default=noprint_wrappers=1", file}).trim();
  }

  private void doTest(MediaProfileSpecType profile, String extension, String expectedTag)
      throws Exception {

    File source = new File(getDefaultOutputFile("-source-h265.mp4"));
    Assume.assumeTrue("Cannot generate the H.265 source file (no x265enc nor libx265)",
        generateSource(source));

    String recordingFile = getRecordUrl(extension);

    MediaPipeline mp = kurentoClient.createMediaPipeline();

    // useEncodedMedia is required to avoid decoding the file in the PlayerEndpoint
    PlayerEndpoint player =
        new PlayerEndpoint.Builder(mp, "file://" + source.getAbsolutePath()).useEncodedMedia()
            .build();
    RecorderEndpoint recorder = new RecorderEndpoint.Builder(mp, recordingFile)
        .withMediaProfile(profile).withVideoCodec(RecorderVideoCodec.H265).build();

    final AtomicInteger transcodingEvents = new AtomicInteger();
    EventListener<MediaTranscodingStateChangedEvent> listener =
        new EventListener<MediaTranscodingStateChangedEvent>() {
          @Override
          public void onEvent(MediaTranscodingStateChangedEvent event) {
            if (event.getState() == MediaTranscodingState.TRANSCODING) {
              transcodingEvents.incrementAndGet();
            }
          }
        };
    player.addMediaTranscodingStateChangedListener(listener);
    recorder.addMediaTranscodingStateChangedListener(listener);

    final CountDownLatch stoppedLatch = new CountDownLatch(1);
    recorder.addStoppedListener(new EventListener<StoppedEvent>() {
      @Override
      public void onEvent(StoppedEvent event) {
        stoppedLatch.countDown();
      }
    });

    player.connect(recorder, MediaType.VIDEO);
    recorder.record();
    player.play();

    Thread.sleep(TimeUnit.SECONDS.toMillis(FILE_SECONDS + 1));
    recorder.stopAndWait();
    Assert.assertTrue("Recorder did not stop",
        stoppedLatch.await(10, TimeUnit.SECONDS));

    mp.release();

    Assert.assertEquals("The video must be recorded as-is, without transcoding", 0,
        transcodingEvents.get());

    AssertMedia.assertCodecs(recordingFile, "HEVC", null);
    AssertMedia.assertGeneralDuration(recordingFile, TimeUnit.SECONDS.toMillis(FILE_SECONDS),
        THRESHOLD_MS);

    String path = recordingFile.substring(recordingFile.indexOf("://") + 3);
    String codec = probe(path, "stream=codec_name,codec_tag_string");
    Assert.assertTrue("Unexpected codec: " + codec, codec.contains("codec_name=hevc"));
    if (expectedTag != null) {
      Assert.assertTrue("Unexpected codec tag: " + codec,
          codec.contains("codec_tag_string=" + expectedTag));
    }
  }

}
