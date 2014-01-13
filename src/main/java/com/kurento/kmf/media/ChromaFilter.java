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
package com.kurento.kmf.media;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * ChromaFilter interface. This type of {@code Filter} makes transparent a
 * colour range in the top layer, revealing another image behind
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 */
public interface ChromaFilter extends Filter {

	/**
	 * Sets the image to show on the detected chroma surface.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 */
	void setBackground(String uri);

	/**
	 * Sets the image to show on the detected chroma surface.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 */
	void setBackground(URI uri);

	/**
	 * Sets the image to show on the detected chroma surface.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly
	 */
	void setBackground(String uri, Continuation<Void> cont);

	/**
	 * Sets the image to show on the detected chroma surface.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly
	 */
	void setBackground(URI uri, Continuation<Void> cont);

	/**
	 * Builder for the {@link ChromaFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 3.0.1
	 */
	public interface ChromaFilterBuilder extends
			MediaObjectBuilder<ChromaFilterBuilder, ChromaFilter> {
		// No special method for the builder at this level

		/**
		 * Sets the image to be used to replace the detected background
		 * 
		 * @param uri
		 *            of the file
		 * @return The builder
		 * @throws URISyntaxException
		 *             if the URI passed as parameter does not comply with <a
		 *             href
		 *             ="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
		 */
		ChromaFilterBuilder withBackgroundImage(String uri)
				throws URISyntaxException;

		/**
		 * Sets the image to be used to replace the detected background
		 * 
		 * @param uri
		 *            of the file
		 * @return The builder
		 */
		ChromaFilterBuilder withBackgroundImage(URI uri);

	}

}
