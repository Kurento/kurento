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
package org.kurento.test.services;

import static org.kurento.common.PropertiesManager.getProperty;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.test.client.Browser;

/**
 * Nodes in Selenium Grid testing.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class Node {

	private final int DEFAULT_MAX_INSTANCES = 1;
	private Logger log = LoggerFactory.getLogger(Node.class);

	public final String REMOTE_FOLDER = "kurento-test";
	public final String REMOTE_PID_FILE = "node-pid";

	private String address;
	private String login;
	private String password;
	private Browser browser;
	private int maxInstances;
	private boolean overwrite;
	private String video;
	private String audio;
	private RemoteHost remoteHost;
	private String home;
	private String tmpFolder;

	public Node(String address, Browser browser, String video, String audio) {
		this(address, browser);
		setVideo(video);
		setAudio(audio);
	}

	public Node(String address, Browser browser, String video) {
		this(address, browser);
		setVideo(video);
	}

	public Node(String address, Browser browser) {
		setAddress(address);
		setMaxInstances(DEFAULT_MAX_INSTANCES);
		setOverwrite(false);
		setLogin(getProperty("test.node.login"));
		setPassword(getProperty("test.node.passwd"));
		setBrowser(browser);
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getMaxInstances() {
		return maxInstances;
	}

	public void setMaxInstances(int maxInstances) {
		this.maxInstances = maxInstances;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public Browser getBrowser() {
		return browser;
	}

	public void setBrowser(Browser browser) {
		this.browser = browser;
	}

	public String getVideo() {
		return video;
	}

	public String getAudio() {
		return audio;
	}

	public String getRemoteVideo() {
		String remoteVideo = null;
		if (video != null) {
			File file = new File(video);
			remoteVideo = getHome() + "/" + REMOTE_FOLDER + "/"
					+ file.getName();
		}
		return remoteVideo;
	}

	public void setVideo(String video) {
		this.video = video;
	}

	public void setAudio(String audio) {
		this.audio = audio;
	}

	public void startRemoteHost() {
		remoteHost = new RemoteHost(getAddress(), getLogin(), getPassword());
		remoteHost.start();
		setTmpFolder(remoteHost.createTmpFolder());
	}

	public void stopRemoteHost() {
		remoteHost.stop();
	}

	public RemoteHost getRemoteHost() {
		return remoteHost;
	}

	public String getTmpFolder() {
		return tmpFolder;
	}

	public void setTmpFolder(String tmpFolder) {
		this.tmpFolder = tmpFolder;
	}

	public String getHome() {
		if (home == null) {
			if (getRemoteHost() == null) {
				startRemoteHost();
			}
			// OverThere SCP need absolute path, so home path must be known
			try {
				home = getRemoteHost().execAndWaitCommandNoBr("echo", "~");
			} catch (IOException e) {
				log.error("Exception reading remote home " + e.getClass()
						+ " ... returning default home value: ~");
				home = "~";
			}
		}
		return home;
	}

}
