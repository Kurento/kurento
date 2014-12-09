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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.kurento.test.services.RemoteHost;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * Internal utility for killing some of the processes of a user in a remote node
 * (for manual testing/debug purposes).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillSomeProcesses {

	private static final String ID = "id : http://";

	public static void main(String[] args) throws IOException {
		KillSomeProcesses killAllProcesses = new KillSomeProcesses();
		killAllProcesses.killAll();
	}

	public void killAll() throws IOException {
		InputStream inputStream = this.getClass().getClassLoader()
				.getResourceAsStream("nodes-to-be-killed.txt");
		List<String> nodeList = CharStreams.readLines(new InputStreamReader(
				inputStream, Charsets.UTF_8));

		for (String line : nodeList) {
			if (line.startsWith(ID)) {
				String node = line.substring(ID.length(), line.indexOf(":555"));
				RemoteHost remoteHost = new RemoteHost(node,
						getProperty("test.node.login"),
						getProperty("test.node.passwd"));
				remoteHost.start();
				remoteHost.execCommand("kill", "-9", "-1");
			}
		}
	}
}
