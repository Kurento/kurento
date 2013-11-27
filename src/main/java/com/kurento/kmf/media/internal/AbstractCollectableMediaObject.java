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

import static com.kurento.kms.thrift.api.KmsMediaServerConstants.DEFAULT_GARBAGE_COLLECTOR_PERIOD;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kms.thrift.api.KmsMediaObjectConstants;

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

	private final int garbagePeriod;

	/**
	 * @param ref
	 */
	public AbstractCollectableMediaObject(MediaObjectRef ref) {
		super(ref);
		this.garbagePeriod = DEFAULT_GARBAGE_COLLECTOR_PERIOD;
	}

	/**
	 * @param ref
	 * @param params
	 */
	public AbstractCollectableMediaObject(MediaObjectRef ref,
			Map<String, MediaParam> params) {
		super(ref, params);
		MediaObjectConstructorParam objConstructorParam = (MediaObjectConstructorParam) params
				.get(KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE);
		if (objConstructorParam != null) {
			this.garbagePeriod = objConstructorParam
					.getGarbageCollectorPeriod();
		} else {
			this.garbagePeriod = DEFAULT_GARBAGE_COLLECTOR_PERIOD;
		}
	}

	@Override
	protected void init() {
		if (garbagePeriod > 0) {
			distributedGarbageCollector.registerReference(
					objectRef.getThriftRef(), garbagePeriod);
		}

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

	protected static abstract class AbstractCollectableMediaObjectBuilder<T extends AbstractCollectableMediaObjectBuilder<T, E>, E extends MediaObject>
			extends AbstractMediaObjectBuilder<T, E> {

		private final MediaObjectConstructorParam param = new MediaObjectConstructorParam();

		protected AbstractCollectableMediaObjectBuilder(
				final String elementType, final MediaPipeline pipeline) {
			super(elementType, pipeline);
		}

		public final T withGarbagePeriod(int period) {
			param.setGarbageCollectorPeriod(period);
			params.put(KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
					param);
			return self();
		}

	}

}
