/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.kmf.content;

import com.kurento.kmf.media.RtpEndpoint;

/**
 * A Media Session where the content is transmitted using RTP.
 * 
 * This interface defines a media session where the content is transmitted using
 * the RTP protocol. It inherits from {@link SdpContentSession}.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 * @see <a href="http://en.wikipedia.org/wiki/Real-time_Transport_Protocol">RTP
 *      Protocol</a>
 */
public interface RtpContentSession extends SdpContentSession {
	/**
	 * TODO
	 */
	void start(RtpEndpoint endpoint);
}
