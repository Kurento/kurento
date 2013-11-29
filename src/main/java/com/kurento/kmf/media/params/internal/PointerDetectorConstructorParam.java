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
package com.kurento.kmf.media.params.internal;

import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;

import java.util.HashSet;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindow;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindowSet;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@ProvidesMediaParam(type = CONSTRUCTOR_PARAMS_DATA_TYPE)
public final class PointerDetectorConstructorParam extends
		AbstractThriftSerializedMediaParam {

	private final Set<KmsMediaPointerDetectorWindow> windows = new HashSet<KmsMediaPointerDetectorWindow>();

	public void addDetectorWindow(final String id, final int height,
			final int width, final int upperRightX, final int upperRightY) {
		final KmsMediaPointerDetectorWindow window = new KmsMediaPointerDetectorWindow(
				upperRightX, upperRightY, height, width, id);
		windows.add(window);
	}

	public PointerDetectorConstructorParam() {
		super(CONSTRUCTOR_PARAMS_DATA_TYPE);
	}

	@Override
	protected TProtocol serializeDataToThrift(final TProtocol pr) {
		final KmsMediaPointerDetectorWindowSet kmsWindowSet = new KmsMediaPointerDetectorWindowSet();
		kmsWindowSet.setWindows(windows);

		try {
			kmsWindowSet.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		final KmsMediaPointerDetectorWindowSet kmsParams = new KmsMediaPointerDetectorWindowSet();
		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		for (final KmsMediaPointerDetectorWindow kmsWindow : kmsParams
				.getWindows()) {
			windows.add(kmsWindow.deepCopy());
		}
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!obj.getClass().equals(this.getClass())) {
			return false;
		}

		PointerDetectorConstructorParam param = (PointerDetectorConstructorParam) obj;

		return this.windows.equals(param.windows);
	}

	@Override
	public int hashCode() {
		return this.windows.hashCode();
	}

}
