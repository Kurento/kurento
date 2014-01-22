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

import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.google.common.base.Objects;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.PointerDetectorAdvFilter;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaImagePoint;
import com.kurento.kms.thrift.api.KmsMediaImageRegion;
import com.kurento.kms.thrift.api.KmsMediaPointerDetector2ConstructorParams;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindow;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindowSet;

/**
 * Class used during the build of a {@link PointerDetectorAdvFilter}. Users may
 * add as many {@link PointerDetectorWindowMediaParam} as needed, in order to
 * configure the areas in which the filter will throw events, when the pointer
 * enters or exits each one of the configured windows.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 * 
 */
@ProvidesMediaParam(type = CONSTRUCTOR_PARAMS_DATA_TYPE)
public final class PointerDetectorAdvConstructorParam extends
		AbstractThriftSerializedMediaParam {

	private final Set<KmsMediaPointerDetectorWindow> windows = newHashSet();

	private final KmsMediaImageRegion calibrationRegion = new KmsMediaImageRegion();

	/**
	 * Adds a new detector window, represented by a simple square or rectangle
	 * in the image.
	 * 
	 * @param window
	 * 
	 */
	public void addDetectorWindow(PointerDetectorWindowMediaParam window) {
		KmsMediaPointerDetectorWindow kmsWindow = new KmsMediaPointerDetectorWindow(
				window.getUpperRightX(), window.getUpperRightY(),
				window.getWidth(), window.getHeight(), window.getId());
		kmsWindow.setActiveOverlayImageUri(window.getActiveImageUri()
				.toString());
		kmsWindow.setInactiveOverlayImageUri(window.getInactiveImageUri()
				.toString());
		kmsWindow.setOverlayTransparency(window.getImageTransparency());
		windows.add(kmsWindow);
	}

	/**
	 * Framework intended constructor
	 */
	public PointerDetectorAdvConstructorParam() {
		super(CONSTRUCTOR_PARAMS_DATA_TYPE);
	}

	/**
	 * 
	 * @param region
	 */
	public PointerDetectorAdvConstructorParam(WindowParam region) {
		this();
		this.calibrationRegion.height = region.getHeight();
		this.calibrationRegion.width = region.getWidth();
		this.calibrationRegion.setPoint(new KmsMediaImagePoint(region
				.getUpperRightX(), region.getUpperRightY()));
	}

	@Override
	protected TProtocol serializeDataToThrift(final TProtocol pr) {
		final KmsMediaPointerDetectorWindowSet kmsWindowSet = new KmsMediaPointerDetectorWindowSet();
		kmsWindowSet.setWindows(windows);

		final KmsMediaPointerDetector2ConstructorParams constructorParams = new KmsMediaPointerDetector2ConstructorParams(
				calibrationRegion);
		constructorParams.setWindowSet(kmsWindowSet);
		try {
			constructorParams.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		final KmsMediaPointerDetector2ConstructorParams kmsParams = new KmsMediaPointerDetector2ConstructorParams();
		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		for (final KmsMediaPointerDetectorWindow kmsWindow : kmsParams.windowSet
				.getWindows()) {
			windows.add(kmsWindow.deepCopy());
		}

		this.calibrationRegion.width = kmsParams.colorCalibrationRegion.width;
		this.calibrationRegion.height = kmsParams.colorCalibrationRegion.height;
		this.calibrationRegion.point = kmsParams.colorCalibrationRegion.point
				.deepCopy();
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

		PointerDetectorAdvConstructorParam param = (PointerDetectorAdvConstructorParam) obj;

		return Objects.equal(calibrationRegion, param.calibrationRegion)
				&& Objects.equal(windows, param.windows);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.calibrationRegion, this.windows);
	}

}
