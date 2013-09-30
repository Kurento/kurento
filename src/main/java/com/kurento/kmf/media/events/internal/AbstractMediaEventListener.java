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
package com.kurento.kmf.media.events.internal;

import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;

public abstract class AbstractMediaEventListener<T extends MediaEvent>
		implements MediaEventListener<T> {

	@Override
	public abstract void onEvent(T event);

	@SuppressWarnings("unchecked")
	public void internalOnEvent(MediaEvent event) {
		// TODO try to replace this internal
		// TODO throw in different thread, in order not to block due to user
		// implementation
		onEvent((T) event);
	}
}
