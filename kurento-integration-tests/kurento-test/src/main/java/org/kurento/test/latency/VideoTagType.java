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
package org.kurento.test.latency;

/**
 * Video tag (local, remote).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public enum VideoTagType {
	LOCAL, REMOTE;

	public static String localId;
	public static String remoteId;

	public String getColor() {
		switch (this) {
		case LOCAL:
			return "return kurentoTest.colorInfo['" + localId
					+ "'].changeColor;";
		case REMOTE:
		default:
			return "return kurentoTest.colorInfo['" + remoteId
					+ "'].changeColor;";
		}
	}

	public String getTime() {
		switch (this) {
		case LOCAL:
			return "return kurentoTest.colorInfo['" + localId
					+ "'].changeTime;";
		case REMOTE:
		default:
			return "return kurentoTest.colorInfo['" + remoteId
					+ "'].changeTime;";
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

	public String getName() {
		switch (this) {
		case LOCAL:
			return "local";
		case REMOTE:
		default:
			return "remote";
		}
	}

	public String getId() {
		if (localId == null || remoteId == null) {
			throw new RuntimeException(
					"You must specify local/remote video tag id in order to perform latency control");
		}

		switch (this) {
		case LOCAL:
			return localId;
		case REMOTE:
		default:
			return remoteId;
		}
	}

	public static void setLocalId(String id) {
		localId = id;
	}

	public static void setRemoteId(String id) {
		remoteId = id;
	}

}
