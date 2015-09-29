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
package org.kurento.test.grid;

import java.io.File;
import java.io.IOException;

import org.kurento.test.browser.BrowserType;
import org.kurento.test.services.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nodes in Selenium Grid testing.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class GridNode {

	private Logger log = LoggerFactory.getLogger(GridNode.class);

	private String host;
	private BrowserType browserType;
	private int maxInstances = 1;
	private boolean overwrite = false;
	private boolean started = false;
	private SshConnection ssh;
	private String home;
	private String tmpFolder;

	public GridNode(String host, BrowserType browserType, int maxInstances) {
		this.host = host;
		this.browserType = browserType;
		this.maxInstances = maxInstances;
		this.ssh = new SshConnection(host);
	}

	public GridNode(String host, BrowserType browserType, int maxInstances, String login, String passwd, String pem) {
		this.host = host;
		this.browserType = browserType;
		this.maxInstances = maxInstances;
		this.ssh = new SshConnection(host, login, passwd, pem);
	}

	public String getRemoteVideo(String video) {
		String remoteVideo = null;
		File file = new File(video);
		remoteVideo = getHome() + "/" + GridHandler.REMOTE_FOLDER + "/" + file.getName();
		return remoteVideo;
	}

	public void startSsh() {
		ssh.start();
		setTmpFolder(ssh.createTmpFolder());
	}

	public void stopSsh() {
		ssh.stop();
	}

	public SshConnection getSshConnection() {
		return ssh;
	}

	public String getTmpFolder() {
		return tmpFolder;
	}

	public void setTmpFolder(String tmpFolder) {
		this.tmpFolder = tmpFolder;
	}

	public String getHome() {
		if (home == null) {
			// OverThere SCP need absolute path, so home path must be known
			try {
				home = getSshConnection().execAndWaitCommandNoBr("echo", "~");
			} catch (IOException e) {
				log.error("Exception reading remote home " + e.getClass() + " ... returning default home value: ~");
				home = "~";
			}
		}
		return home;
	}

	public String getHost() {
		return host;
	}

	public BrowserType getBrowserType() {
		return browserType;
	}

	public int getMaxInstances() {
		return maxInstances;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

}
