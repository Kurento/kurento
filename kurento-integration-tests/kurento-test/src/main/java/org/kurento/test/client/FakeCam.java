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
package org.kurento.test.client;

import org.kurento.test.Shell;

/**
 * Fake cam singleton.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="https://github.com/umlaeute/v4l2loopback">v4l2loopback</a>
 */
public class FakeCam {

	private static FakeCam singleton = null;

	/**
	 * From 1 to NUM_FAKE_CAMS
	 */
	private static int NUM_FAKE_CAMS = 4;

	private int currentCam;

	public static FakeCam getSingleton() {
		if (singleton == null) {
			singleton = new FakeCam();
		}
		return singleton;
	}

	public FakeCam() {
		this.currentCam = 0;
	}

	public int getCam() {
		this.currentCam++;
		if (this.currentCam > NUM_FAKE_CAMS) {
			throw new IndexOutOfBoundsException();
		}
		return this.currentCam;
	}

	public void launchCam(String video) {
		Shell.runAndWait("sh", "-c", "gst-launch filesrc location=" + video
				+ " ! decodebin2 ! v4l2sink device=/dev/video" + getCam());
	}

}
