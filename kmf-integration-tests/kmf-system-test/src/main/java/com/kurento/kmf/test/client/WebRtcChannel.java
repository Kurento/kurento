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
package com.kurento.kmf.test.client;

/**
 * Type of channel in WebRTC communications (audio, video, or both).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public enum WebRtcChannel {
	VIDEO_ONLY, AUDIO_ONLY, AUDIO_AND_VIDEO;

	public boolean getAudio() {
		switch (this) {
		case VIDEO_ONLY:
			return false;
		case AUDIO_ONLY:
			return true;
		case AUDIO_AND_VIDEO:
		default:
			return true;
		}
	}

	public boolean getVideo() {
		switch (this) {
		case VIDEO_ONLY:
			return true;
		case AUDIO_ONLY:
			return false;
		case AUDIO_AND_VIDEO:
		default:
			return true;
		}
	}
}
