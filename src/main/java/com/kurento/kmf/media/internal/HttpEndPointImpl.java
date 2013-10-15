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

import static com.kurento.kms.thrift.api.KmsMediaHttpEndPointTypeConstants.GET_URL;
import static com.kurento.kms.thrift.api.KmsMediaHttpEndPointTypeConstants.TYPE_NAME;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.internal.StringMediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class HttpEndPointImpl extends AbstractSessionEndPoint implements
		HttpEndPoint {

	public HttpEndPointImpl(MediaElementRef endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String getUrl() {
		StringMediaParam result = (StringMediaParam) invoke(GET_URL);
		return result.getString();
	}

	/* ASYNC */

	@Override
	public void getUrl(final Continuation<String> cont) {
		invoke(GET_URL, new StringContinuationWrapper(cont));
	}

}
