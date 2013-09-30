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

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kms.thrift.api.KmsEvent;

public abstract class AbstractMediaEvent implements MediaEvent {

	@Autowired
	protected ApplicationContext applicationContext;

	private final MediaObjectRefDTO sourceRef;
	private MediaObject source;
	private final String type;

	// TODO: should not be visible to final developer
	public AbstractMediaEvent(KmsEvent event) {
		this.sourceRef = fromThrift(event.source);
		this.type = event.type;
	}

	@Override
	public MediaObject getSource() {
		if (source == null) {
			source = (MediaObject) applicationContext.getBean("mediaObject",
					sourceRef);
		}
		return source;
	}

	@Override
	public String getType() {
		return this.type;
	}

	// TODO: should not be visible to final developer
	public abstract void deserializeData(KmsEvent event);

}
