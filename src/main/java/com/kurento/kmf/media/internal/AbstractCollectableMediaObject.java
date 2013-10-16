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

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;

/**
 * Abstract class that encapsulates the registration of a {@link MediaObject} in
 * the distributed garbage collector. Objects that extend this class will be
 * registered in the DGC, and periodical keepalives will be sent to the Media
 * Server
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public abstract class AbstractCollectableMediaObject extends
		AbstractMediaObject {

	@Autowired
	private DistributedGarbageCollector distributedGarbageCollector;

	/**
	 * @param ref
	 */
	public AbstractCollectableMediaObject(MediaObjectRef ref) {
		super(ref);
	}

	@Override
	protected void init() {
		distributedGarbageCollector.registerReference(objectRef.getThriftRef());
		super.init();
	}

	@Override
	public void release() {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		super.release();
	}

	@Override
	public void release(final Continuation<Void> cont) {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		super.release(cont);
	}

	@Override
	protected void finalize() {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		super.finalize();
	}
}
