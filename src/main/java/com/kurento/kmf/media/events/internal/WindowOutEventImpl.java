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

import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.EVENT_WINDOW_OUT;

import com.kurento.kmf.media.events.WindowOutEvent;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kmf.media.params.internal.StringMediaParam;
import com.kurento.kms.thrift.api.KmsMediaEvent;

/**
 * Default implementation of {@link WindowOutEvent}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@ProvidesMediaEvent(type = EVENT_WINDOW_OUT)
public class WindowOutEventImpl extends AbstractMediaEvent<StringMediaParam>
		implements WindowOutEvent {

	/**
	 * @param event
	 *            The Thrift event received form the server.
	 */
	public WindowOutEventImpl(KmsMediaEvent event) {
		super(event);
	}

	@Override
	public String getWindowId() {
		return this.getParam().getString();
	}

}
