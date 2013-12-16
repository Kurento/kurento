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
package com.kurento.kmf.media.internal;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPad;
import com.kurento.kmf.media.MediaType;
import com.kurento.kmf.media.internal.refs.MediaPadRef;

public abstract class MediaPadImpl extends AbstractMediaObject implements
		MediaPad {

	public MediaPadImpl(MediaPadRef objectRef) {
		super(objectRef);
	}

	@Override
	public MediaElement getMediaElement() {
		return (MediaElement) getParent();
	}

	@Override
	public MediaType getMediaType() {
		return ((MediaPadRef) objectRef).getType();
	}

	@Override
	public String getMediaDescription() {
		return ((MediaPadRef) objectRef).getMediaDescription();
	}

	@Override
	public void getMediaElement(final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {
		super.getParent(cont);
	}

	@Override
	public void getMediaType(Continuation<MediaType> cont) {
		cont.onSuccess(this.getMediaType());
	}

	@Override
	public void getMediaDescription(Continuation<String> cont) {
		cont.onSuccess(this.getMediaDescription());
	}
}
