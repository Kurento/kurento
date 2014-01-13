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

import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.SET_BACKGROUND_PARAM_BACKGROUND_TYPE;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.google.common.base.Objects;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.ChromaFilter;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaChromaBackgroundImage;

/**
 * Background image parameter to be used for the {@link ChromaFilter}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 * 
 */
@ProvidesMediaParam(type = SET_BACKGROUND_PARAM_BACKGROUND_TYPE)
public class ChromaBackgroundImageParam extends
		AbstractThriftSerializedMediaParam {

	private URI imageUri;

	/**
	 * Default constructor. This constructor is intended to be used by the
	 * framework.
	 */
	public ChromaBackgroundImageParam() {
		super(SET_BACKGROUND_PARAM_BACKGROUND_TYPE);
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
		KmsMediaChromaBackgroundImage kmsParams = new KmsMediaChromaBackgroundImage(
				imageUri.toString());

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
		KmsMediaChromaBackgroundImage kmsParams = new KmsMediaChromaBackgroundImage();

		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		try {
			this.imageUri = new URI(kmsParams.uri);
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

		final ChromaBackgroundImageParam param = (ChromaBackgroundImageParam) obj;

		return Objects.equal(this.imageUri, param.imageUri);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.imageUri);
	}

}
