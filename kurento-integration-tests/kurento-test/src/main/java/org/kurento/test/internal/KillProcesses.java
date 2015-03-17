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

import org.kurento.test.services.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility for killing all the processes of a user in a remote node
 * (for manual testing/debug purposes).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillProcesses {

	public static Logger log = LoggerFactory.getLogger(KillProcesses.class);

	public static void main(String[] args) throws IOException {
		for (String node : args) {
			if (SshConnection.ping(node)) {
				SshConnection remoteHost = new SshConnection(node);
				remoteHost.start();
				remoteHost.execCommand("kill", "-9", "-1");
			} else {
				log.error("Node down {}", node);
			}
		}
	}
}
