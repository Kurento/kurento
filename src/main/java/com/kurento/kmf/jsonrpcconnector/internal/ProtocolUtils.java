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
package com.kurento.kmf.jsonrpcconnector.internal;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 */
public final class ProtocolUtils {

	/**
	 * Encodings accepted in JSON (UTF-8, UTF-16BE/LE, UTF-32BE/LE).
	 */
	private static final String UTF8 = "UTF-8";
	private static final String UTF16BE = "UTF-16BE";
	private static final String UTF16LE = "UTF-16LE";
	private static final String UTF32BE = "UTF-32BE";
	private static final String UTF32LE = "UTF-32LE";

	/**
	 * Reads inputStream (from request) and detects incoming JSON encoding.
	 * 
	 * @param inputStream
	 *            Input Stream from request
	 * @return String identifier for detected JSON (UTF8, UTF16LE, ...)
	 * @throws IOException
	 *             Exception while parsing JSON
	 */
	public static String detectJsonEncoding(InputStream inputStream)
			throws IOException {
		inputStream.mark(4);
		int mask = 0;
		for (int count = 0; count < 4; count++) {
			int r = inputStream.read();
			if (r == -1) {
				break;
			}
			mask = mask << 1;
			mask |= (r == 0) ? 0 : 1;
		}
		inputStream.reset();
		return match(mask);
	}

	/**
	 * Match recovered mask to String identifier (UTF8, UTF16LE, ...).
	 * 
	 * @param mask
	 *            Mask from detectJsonEncoding method
	 * @return String identifier for the detected JSON encoding
	 */
	private static String match(int mask) {
		switch (mask) {
		case 1:
			return UTF32BE;
		case 5:
			return UTF16BE;
		case 8:
			return UTF32LE;
		case 10:
			return UTF16LE;
		case 15:
			return UTF8;
		default:
			return null;
		}
	}

}
