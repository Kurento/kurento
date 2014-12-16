/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kurento.test.services.RemoteHost;

/**
 * Internal utility for killing the active processes of a user in the Selenium
 * Grid hub (for manual testing/debug purposes).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillActiveProcesses {

	private static final String REGEX = "id : http://(.*?), OS";

	public static void main(String[] args) throws IOException {
		KillActiveProcesses killActive = new KillActiveProcesses();

		for (String url : args) {
			String contents = killActive.readContents(url);

			Pattern p = Pattern.compile(REGEX);
			Matcher m = p.matcher(contents);

			String node;
			while (m.find()) {
				node = m.group();
				node = node.substring(12, node.lastIndexOf(":"));
				System.out.println("Killing " + node);
				killActive.kill(node);
			}
		}

	}

	public void kill(String node) throws IOException {
		RemoteHost remoteHost = new RemoteHost(node,
				getProperty("test.node.login"), getProperty("test.node.passwd"));
		remoteHost.start();
		remoteHost.execCommand("kill", "-9", "-1");
	}

	public String readContents(String address) throws IOException {
		StringBuilder contents = new StringBuilder(2048);
		BufferedReader br = null;
		try {
			URL url = new URL(address);
			br = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = "";
			while (line != null) {
				line = br.readLine();
				contents.append(line);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return contents.toString();
	}

}
