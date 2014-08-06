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
package org.kurento.kmf.test.mediainfo;

import java.io.File;

import org.junit.Assert;

/**
 * 
 * Utility class to assert the expected codecs in a media (video/audio) file.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.3.1
 */
public class AssertMedia {

	public static void assertCodecs(String pathToMedia,
			String expectedVideoCodec, String expectedAudioCodec) {
		MediaInfo info = new MediaInfo();
		info.open(new File(pathToMedia));
		String videoFormat = info.get(MediaInfo.StreamKind.Video, 0, "Format",
				MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String audioFormat = info.get(MediaInfo.StreamKind.Audio, 0, "Format",
				MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		info.close();

		Assert.assertEquals("Expected video codec is " + expectedVideoCodec
				+ " and the recorded video is " + videoFormat,
				expectedVideoCodec, videoFormat);
		Assert.assertEquals("Expected audio codec is " + expectedAudioCodec
				+ " and the recorded video is " + audioFormat,
				expectedAudioCodec, audioFormat);
	}

}
