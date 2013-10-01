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
package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;

@RecorderService(name = "MyRecorderHandlerRecordWithRedirect", path = "/recorder-record-with-redirect", redirect = true)
public class MyRecorderHandlerRecordWithRedirect implements RecorderHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyRecorderHandlerRecordWithRedirect.class);

	@Override
	public void onRecordRequest(RecordRequest recordRequest)
			throws ContentException {
		log.debug("onRecordRequest");
		recordRequest.record("myfile-with-redirect");
	}

	@Override
	public void onContentRecorded(String contentId) {
		log.debug("onContentRecorded");
	}

	@Override
	public void onContentError(String contentId, ContentException exception) {
		log.debug("onContentError " + exception.getMessage());
	}
}
