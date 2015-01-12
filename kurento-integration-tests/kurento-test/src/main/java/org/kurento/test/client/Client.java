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

/**
 * Kind of client (Player, WebRTC, and so on).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public enum Client {
	PLAYER, WEBRTC, ROOM, SCREEN;

	public String toString() {
		switch (this) {
		case PLAYER:
			return "/player.html";
		case ROOM:
			return "/room.html";
		case SCREEN:
			return "/index.html";
		case WEBRTC:
		default:
			return "/webrtc.html";
		}
	}
}
