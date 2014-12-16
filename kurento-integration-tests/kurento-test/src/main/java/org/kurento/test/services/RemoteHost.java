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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.xebialabs.overthere.CmdLine;
import com.xebialabs.overthere.ConnectionOptions;
import com.xebialabs.overthere.OperatingSystemFamily;
import com.xebialabs.overthere.Overthere;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.OverthereProcess;
import com.xebialabs.overthere.ssh.SshConnectionBuilder;
import com.xebialabs.overthere.ssh.SshConnectionType;

/**
 * SSH connection to a remote host.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class RemoteHost {

	public static Logger log = LoggerFactory.getLogger(RemoteHost.class);
	public static final String DEFAULT_TMP_FOLDER = "/tmp";

	private static final int NODE_INITIAL_PORT = 5555;
	private static final int PING_TIMEOUT = 2; // seconds

	private String host;
	private String login;
	private String passwd;

	private OverthereConnection connection;

	public RemoteHost(String host, String login, String passwd) {
		this.host = host;
		this.login = login;
		this.passwd = passwd;
	}

	public String createTmpFolder() {
		String remoteFolder;
		try {
			do {
				remoteFolder = DEFAULT_TMP_FOLDER + "/" + System.nanoTime();
			} while (exists(remoteFolder));
			execAndWaitCommand("mkdir", remoteFolder);
		} catch (IOException e) {
			remoteFolder = DEFAULT_TMP_FOLDER;
		}

		log.debug("Remote folder to store temporal files in node {}: {} ",
				host, remoteFolder);
		return remoteFolder;
	}

	public void getFile(String targetFile, String origFile) {
		OverthereFile motd = connection.getFile(origFile);
		InputStream is = motd.getInputStream();
		try {
			Files.copy(is, Paths.get(targetFile));
			is.close();
		} catch (IOException e) {
			log.error("Exception getting file: {} to {} ()", origFile,
					targetFile, e.getMessage());
		}
	}

	public void scp(String origFile, String targetFile) throws IOException {
		OverthereFile motd = connection.getFile(targetFile);
		OutputStream w = motd.getOutputStream();

		byte[] origBytes = Files.readAllBytes(Paths.get(origFile));
		w.write(origBytes);
		w.close();
	}

	public void start() {
		ConnectionOptions options = new ConnectionOptions();
		options.set(ConnectionOptions.USERNAME, login);
		options.set(ConnectionOptions.PASSWORD, passwd);
		options.set(ConnectionOptions.ADDRESS, host);
		options.set(ConnectionOptions.OPERATING_SYSTEM,
				OperatingSystemFamily.UNIX);
		options.set(SshConnectionBuilder.CONNECTION_TYPE, SshConnectionType.SCP);

		connection = Overthere.getConnection(SshConnectionBuilder.SSH_PROTOCOL,
				options);

	}

	public void stop() {
		if (connection != null) {
			connection.close();
		}
	}

	public void execCommand(String... command) throws IOException {
		if (connection.canStartProcess()) {
			connection.startProcess(CmdLine.build(command));
		}
	}

	public int runAndWaitCommand(String... command) throws IOException {
		return connection.execute(CmdLine.build(command));
	}

	public String execAndWaitCommand(String... command) throws IOException {
		OverthereProcess process = connection.startProcess(CmdLine
				.build(command));
		return CharStreams.toString(new InputStreamReader(process.getStdout(),
				"UTF-8"));
	}

	public String execAndWaitCommandNoBr(String... command) throws IOException {
		return execAndWaitCommand(command).replace("\n", "").replace("\r", "");
	}

	public boolean exists(String fileOrFolder) throws IOException {
		String output = execAndWaitCommand("file", fileOrFolder);
		return !output.contains("ERROR");
	}

	public int getFreePort() throws IOException {
		int port = NODE_INITIAL_PORT - 1;
		String output;
		do {
			port++;
			output = execAndWaitCommand("netstat", "-auxn");
		} while (output.contains(":" + port));
		return port;
	}

	public static boolean ping(String ipAddress) {
		return ping(ipAddress, RemoteHost.PING_TIMEOUT);
	}

	public static boolean ping(final String ipAddress, int timeout) {
		final CountDownLatch latch = new CountDownLatch(1);

		Thread t = new Thread() {
			public void run() {
				try {
					String[] command = { "ping", "-c", "1", ipAddress };
					Process p = new ProcessBuilder(command)
							.redirectErrorStream(true).start();
					CharStreams.toString(new InputStreamReader(p
							.getInputStream(), "UTF-8"));
					latch.countDown();
				} catch (Exception e) {
				}
			}
		};
		t.setDaemon(true);
		t.start();

		boolean ping = false;
		try {
			ping = latch.await(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("Exception making ping to {} : {}", ipAddress,
					e.getClass());
		}
		if (!ping) {
			t.interrupt();
		}

		return ping;
	}
}
