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

import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.google.common.base.Objects;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.ChromaFilter;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaChromaBackgroundImage;
import com.kurento.kms.thrift.api.KmsMediaChromaColorCalibrationArea;
import com.kurento.kms.thrift.api.KmsMediaChromaConstructorParams;

/**
 * Constructor parameter object for the {@link ChromaFilter}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 * 
 */
@ProvidesMediaParam(type = CONSTRUCTOR_PARAMS_DATA_TYPE)
public class ChromaConstructorParam extends AbstractThriftSerializedMediaParam {

	private WindowParam window;

	private URI imageUri;

	/**
	 * Default constructor. This constructor is intended to be used by the
	 * framework.
	 */
	public ChromaConstructorParam() {
		super(CONSTRUCTOR_PARAMS_DATA_TYPE);
	}

	private ChromaConstructorParam(Builder builder) {
		this();
		this.window = builder.window;
		this.imageUri = builder.imageUri;
	}

	public WindowParam getCalibrationWindow() {
		return this.window;
	}

	public URI getImageUri() {
		return this.imageUri;
	}

	public void setImageUri(String uri) throws URISyntaxException {
		setImageUri(new URI(uri));
	}

	public void setImageUri(URI uri) {
		this.imageUri = uri;
	}

	@Override
	protected TProtocol serializeDataToThrift(TProtocol pr) {
		KmsMediaChromaConstructorParams kmsParams = new KmsMediaChromaConstructorParams();
		KmsMediaChromaColorCalibrationArea colorCalibrationArea = new KmsMediaChromaColorCalibrationArea(
				this.window.getUpperRightX(), this.window.getUpperRightY(),
				this.window.getWidth(), this.window.getHeight());

		kmsParams.setCalibrationArea(colorCalibrationArea);

		if (imageUri != null) {
			KmsMediaChromaBackgroundImage image = new KmsMediaChromaBackgroundImage(
					imageUri.toString());
			kmsParams.setBackgroundImage(image);
		}

		try {
			kmsParams.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		KmsMediaChromaConstructorParams kmsParams = new KmsMediaChromaConstructorParams();

		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		KmsMediaChromaColorCalibrationArea area = kmsParams
				.getCalibrationArea();
		this.window = new WindowParam(area.x, area.y, area.width, area.height);

		try {
			this.imageUri = new URI(kmsParams.getBackgroundImage().uri);
		} catch (URISyntaxException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(
					"Invalid URI received from server", e, 30000);
		}
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null || this.getClass() != (obj.getClass())) {
			return false;
		}

		final ChromaConstructorParam param = (ChromaConstructorParam) obj;

		return Objects.equal(this.window, param.window)
				&& Objects.equal(this.imageUri, param.imageUri);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.window, this.imageUri);
	}

	public static class Builder {

		private final WindowParam window;

		private URI imageUri;

		/**
		 * Constructor that takes as argument the window where the background
		 * colour is configured
		 * 
		 * @param window
		 *            the window
		 */
		public Builder(WindowParam window) {
			this.window = window;
		}

		/**
		 * Sets the image that will be overlaid where the background colour is
		 * detected
		 * 
		 * @param image
		 *            the uri pointing to the image
		 * @return the builder
		 * @throws URISyntaxException
		 *             if the URI passed as parameter does not comply with <a
		 *             href
		 *             ="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
		 */
		public Builder withBackgoundImage(String image)
				throws URISyntaxException {
			return withBackgoundImage(new URI(image));
		}

		/**
		 * Sets the image that will be overlaid where the background colour is
		 * detected
		 * 
		 * @param image
		 *            the uri pointing to the image
		 * @return the builder
		 */
		public Builder withBackgoundImage(URI image) {
			this.imageUri = image;
			return this;
		}

		public ChromaConstructorParam build() {
			return new ChromaConstructorParam(this);
		}
	}
}
