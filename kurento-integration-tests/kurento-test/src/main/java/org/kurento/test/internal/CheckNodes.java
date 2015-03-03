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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.kurento.test.services.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * Internal test application for assessing the state of hosts for nodes in
 * Selenium Grid.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class CheckNodes {

	public Logger log = LoggerFactory.getLogger(CheckNodes.class);

	public void check() throws IOException {
		InputStream inputStream = this.getClass().getClassLoader()
				.getResourceAsStream("node-list.txt");
		List<String> nodeList = CharStreams.readLines(new InputStreamReader(
				inputStream, Charsets.UTF_8));

		List<String> nodesWithoutXvfb = new ArrayList<String>();
		List<String> nodesWithException = new ArrayList<String>();
		List<String> nodesDown = new ArrayList<String>();
		List<String> nodesOk = new ArrayList<String>();

		for (String node : nodeList) {
			if (SshConnection.ping(node)) {

				SshConnection remoteHost = new SshConnection(node);
				try {
					remoteHost.start();
					int xvfb = remoteHost.runAndWaitCommand("xvfb-run");
					if (xvfb != 2) {
						nodesWithoutXvfb.add(node);
					} else {
						nodesOk.add(node);
					}
					log.info("{} {}", node, xvfb);
				} catch (Exception e) {
					log.error("Exception in node {} : {}", node, e.getClass());
					nodesWithException.add(node);
				} finally {
					remoteHost.stop();
				}
			} else {
				log.error("Node down {}", node);
				nodesDown.add(node);
			}
		}

		log.info("Nodes Ok: {} {}", nodesOk.size(), nodesOk);
		log.info("Nodes without Xvfb: {} {}", nodesWithoutXvfb.size(),
				nodesWithoutXvfb);
		log.info("Nodes with exception: {} {}", nodesWithException.size(),
				nodesWithException);
		log.info("Nodes down: {} {}", nodesDown.size(), nodesDown);
	}

	public static void main(String[] args) throws IOException {
		CheckNodes checkNodes = new CheckNodes();
		checkNodes.check();
	}

}
