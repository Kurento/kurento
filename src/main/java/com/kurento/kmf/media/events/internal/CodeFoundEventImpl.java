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

import static com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants.EVENT_CODE_FOUND;

import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kmf.media.params.internal.EventCodeFoundParam;
import com.kurento.kms.thrift.api.KmsMediaEvent;

@ProvidesMediaEvent(type = EVENT_CODE_FOUND)
public class CodeFoundEventImpl extends AbstractMediaEvent<EventCodeFoundParam>
		implements CodeFoundEvent {

	public CodeFoundEventImpl(KmsMediaEvent event) {
		super(event);
	}

	@Override
	public ZBarFilter getSource() {
		return (ZBarFilter) super.getSource();
	}

	@Override
	public String getCodeType() {
		return this.getParam().getCodeType();
	}

	@Override
	public String getValue() {
		return this.getParam().getValue();
	}
}
