/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.sdp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for handling SDPs.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class SdpUtils {

	private static final String SDP_DELIMITER = "\r\n";

	public static final Logger log = LoggerFactory.getLogger(SdpUtils.class);

	public static String mangleSdp(String sdpIn, String[] removeCodes) {
		String sdpMangled1 = "";
		List<String> indexList = new ArrayList<>();
		for (String line : sdpIn.split(SDP_DELIMITER)) {
			boolean codecFound = false;
			for (String codec : removeCodes) {
				codecFound |= line.contains(codec);
			}
			if (codecFound) {
				String index = line.substring(line.indexOf(":") + 1,
						line.indexOf(" ") + 1);
				indexList.add(index);
			} else {
				sdpMangled1 += line + SDP_DELIMITER;
			}
		}

		String sdpMangled2 = "";
		log.info("indexList " + indexList);
		for (String line : sdpMangled1.split(SDP_DELIMITER)) {
			for (String index : indexList) {
				line = line.replaceAll(index, "");
			}
			sdpMangled2 += line + SDP_DELIMITER;
		}
		return sdpMangled2;
	}

}
