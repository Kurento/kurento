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
package org.kurento.test.color;

/**
 * Video tag (local, remote).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.6
 */
public enum VideoTag {
	LOCAL, REMOTE;

	public String getColor() {
		switch (this) {
		case LOCAL:
			return "return colorInfo[0].rgba;";
		case REMOTE:
		default:
			return "return colorInfo[1].rgba;";
		}
	}

	public String getTime() {
		switch (this) {
		case LOCAL:
			return "return colorInfo[0].time;";
		case REMOTE:
		default:
			return "return colorInfo[1].time;";
		}
	}

	public String toString() {
		switch (this) {
		case LOCAL:
			return "local stream";
		case REMOTE:
		default:
			return "remote stream";
		}
	}
}
