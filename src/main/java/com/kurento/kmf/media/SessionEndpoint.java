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
package com.kurento.kmf.media;

import com.kurento.kmf.media.events.HasMediaSessionStartedListener;
import com.kurento.kmf.media.events.HasMediaSessionTerminatedListener;

/**
 * Session based endpoint. A session is considered to be started when the media
 * exchange starts. On the other hand, sessions terminate when a timeout,
 * defined by the developer, takes place after the connection is lost.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface SessionEndpoint extends Endpoint,
		HasMediaSessionTerminatedListener, HasMediaSessionStartedListener {

}
