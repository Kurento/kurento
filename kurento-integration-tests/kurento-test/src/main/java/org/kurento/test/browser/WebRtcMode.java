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
package org.kurento.test.browser;

/**
 * WebRTC communication mode (send and receive, send only, or receive only).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.4
 */
public enum WebRtcMode {
	SEND_RCV, SEND_ONLY, RCV_ONLY;

	public String getJsFunction() {
		switch (this) {
		case SEND_RCV:
			return "startSendRecv();";
		case SEND_ONLY:
			return "startSendOnly();";
		case RCV_ONLY:
			return "startRecvOnly();";
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public String toString() {
		switch (this) {
		case SEND_RCV:
			return "(SEND & RECEIVE)";
		case SEND_ONLY:
			return "(SEND ONLY)";
		case RCV_ONLY:
			return "(RECEIVE ONLY)";
		default:
			throw new IllegalArgumentException();
		}
	}
}
