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
package com.kurento.kmf.media.internal.pool;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class MediaServerSyncClientPool extends AbstractPool<Client> {

	@Autowired
	private MediaServerSyncClientFactory syncFactory;

	public MediaServerSyncClientPool() {
		super();
	}

	@PostConstruct
	private void init() {
		super.init(syncFactory);
	}
}
