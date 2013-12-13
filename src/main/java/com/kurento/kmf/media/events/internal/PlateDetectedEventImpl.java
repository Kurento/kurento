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

import static com.kurento.kms.thrift.api.KmsMediaPlateDetectorFilterTypeConstants.EVENT_PLATE_DETECTED;

import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kmf.media.params.internal.StringMediaParam;
import com.kurento.kms.thrift.api.KmsMediaEvent;

/**
 * Default implementation of {@link PlateDetectedEvent}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 * 
 */
@ProvidesMediaEvent(type = EVENT_PLATE_DETECTED)
public class PlateDetectedEventImpl extends
		AbstractMediaEvent<StringMediaParam> implements PlateDetectedEvent {

	/**
	 * Constructor to be used by the framework when an event is received by the
	 * server.
	 * 
	 * @param event
	 *            Thrift event received form the server.
	 */
	public PlateDetectedEventImpl(KmsMediaEvent event) {
		super(event);
	}

	@Override
	public String getPlate() {
		return this.getParam().getString();
	}

}
