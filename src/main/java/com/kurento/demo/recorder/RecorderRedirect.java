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
package com.kurento.demo.recorder;

import com.kurento.kmf.content.HttpRecorderService;

/**
 * HTTP Recorder Handler; tunnel strategy (redirect=true); not using JSON-RPC
 * control protocol (useControlProtocol=false).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.1
 */
@HttpRecorderService(path = "/recorderRedirect", redirect = true, useControlProtocol = false)
public class RecorderRedirect extends AbstractBaseRecorder {

	@Override
	protected String getUri() {
		return "file:///tmp/recorderRedirect";
	}

}
