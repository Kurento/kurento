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

import com.kurento.kmf.media.events.DefaultMediaEvent;
import com.kurento.kms.thrift.api.KmsEvent;

/**
 * Default media event that is used when an unknown event type is received from
 * the media server
 * 
 * @author llopez
 * 
 */
public class DefaultMediaEventImpl extends AbstractMediaEvent implements
		DefaultMediaEvent {

	private byte[] data;

	public DefaultMediaEventImpl(KmsEvent event) {
		super(event);
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public void deserializeData(KmsEvent event) {
		// TODO get the params
		// data = event.getEventData();
	}

}
