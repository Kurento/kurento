package org.kurento.kmf.test.grid;

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
import static org.kurento.kmf.common.PropertiesManager.getProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.kurento.kmf.test.services.RemoteHost;

/**
 * Meta test for assessing the state of hosts for nodes in Selenium Grid.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class CheckNodes {

	public static Logger log = LoggerFactory.getLogger(CheckNodes.class);

	// @Test
	public void test() throws IOException {
		InputStream inputStream = this.getClass().getClassLoader()
				.getResourceAsStream("node-list.txt");
		List<String> nodeList = CharStreams.readLines(new InputStreamReader(
				inputStream, Charsets.UTF_8));

		List<String> nodesWithoutXvfb = new ArrayList<String>();
		List<String> nodesWithException = new ArrayList<String>();
		List<String> nodesDown = new ArrayList<String>();
		List<String> nodesOk = new ArrayList<String>();

		for (String node : nodeList) {
			if (RemoteHost.ping(node)) {

				RemoteHost remoteHost = new RemoteHost(node,
						getProperty("test.node.login"),
						getProperty("test.node.passwd"));
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

}
