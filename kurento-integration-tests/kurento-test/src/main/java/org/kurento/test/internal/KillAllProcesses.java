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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * Internal utility for killing all the processes of a user in a remote node
 * (for manual testing/debug purposes).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillAllProcesses {

	public static Logger log = LoggerFactory.getLogger(KillAllProcesses.class);

	public static void main(String[] args) throws IOException {
		KillAllProcesses killAllProcesses = new KillAllProcesses();
		killAllProcesses.killAll();
	}

	public void killAll() throws IOException {

		InputStream inputStream = this.getClass().getClassLoader()
				.getResourceAsStream("node-list.txt");
		List<String> nodeList = CharStreams.readLines(new InputStreamReader(
				inputStream, Charsets.UTF_8));

		for (String node : nodeList) {
			if (RemoteHost.ping(node)) {
				try {
					RemoteHost remoteHost = new RemoteHost(node,
							getProperty("test.node.login"),
							getProperty("test.node.passwd"));
					remoteHost.start();
					remoteHost.execCommand("kill", "-9", "-1");
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				log.error("Node down {}", node);
			}
		}
	}
}
