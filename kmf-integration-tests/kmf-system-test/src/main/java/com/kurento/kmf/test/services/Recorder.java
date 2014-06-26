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
package com.kurento.kmf.test.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.test.Shell;

/**
 * Audio recorder using AVCONV (formerly FFMPEG) and audio quality assessment
 * with PESQ.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.11
 * @see <a href="https://libav.org/avconv.html">AVCONV</a>
 * @see <a href="http://en.wikipedia.org/wiki/PESQ">PESQ</a>
 */
public class Recorder {

	private static Logger log = LoggerFactory.getLogger(Recorder.class);

	private static final String HTTP_TEST_FILES = "http://files.kurento.org";
	private static final String PESQ_RESULTS = "pesq_results.txt";
	private static final String RECORDED_WAV = KurentoMediaServerManager
			.getWorkspace() + "recorded.wav";

	public static void record(int seconds, int sampleRate,
			AudioChannel audioChannel) {
		Shell.run("sh", "-c", "avconv -y -t " + seconds
				+ " -f alsa -i pulse -q:a 0 -ac " + audioChannel + " -ar "
				+ sampleRate + " " + RECORDED_WAV);
	}

	public static float getPesqMos(String audio, int sampleRate) {
		float pesqmos = 0;

		try {
			String pesq = KurentoServicesTestHelper.getTestFilesPath()
					+ "/bin/pesq/PESQ";
			String origWav = "";
			if (audio.startsWith(HTTP_TEST_FILES)) {
				origWav = KurentoServicesTestHelper.getTestFilesPath()
						+ audio.replace(HTTP_TEST_FILES, "");
			} else {
				// Download URL
				origWav = KurentoMediaServerManager.getWorkspace()
						+ "/downloaded.wav";
				URL url = new URL(audio);
				ReadableByteChannel rbc = Channels.newChannel(url.openStream());
				FileOutputStream fos = new FileOutputStream(origWav);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
			}

			Shell.runAndWait(pesq, "+" + sampleRate, origWav, RECORDED_WAV);
			List<String> lines = FileUtils.readLines(new File(PESQ_RESULTS),
					"utf-8");
			pesqmos = Float.parseFloat(lines.get(1).split("\t")[2].trim());
			log.info("PESQMOS " + pesqmos);

			Shell.runAndWait("rm", PESQ_RESULTS);

		} catch (IOException e) {
			log.error("Exception recording local audio", e);
		}

		return pesqmos;
	}

}
