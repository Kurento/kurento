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
package org.kurento.test.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kurento.test.grid.GridHandler;

/**
 * Internal utility for reading a node from a URL.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.0.1
 */
public class GetNodeList {

	public static void main(String[] args) throws IOException {
		List<String> nodeList = new ArrayList<>();

		for (String url : args) {
			String contents = GridHandler.readContents(url);
			Pattern p = Pattern.compile(GridHandler.IPS_REGEX);
			Matcher m = p.matcher(contents);
			while (m.find()) {
				nodeList.add(m.group());
			}
		}
		System.err.println(nodeList);
	}

}
