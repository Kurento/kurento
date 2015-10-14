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
package org.kurento.test.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * System monitor class, used to check the CPU usage, memory, swap, and network
 * of the machine running the tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class SystemMonitor {

	public static final String MONITOR_PORT_PROP = "kms.monitor.port";
	public static final int MONITOR_PORT_DEFAULT = 12345;
	public static final int KMS_WAIT_TIMEOUT = 10; // seconds

	private static final String ERR = "error: ";

	private double prevTotal = 0;
	private double prevIdle = 0;
	private int kmsPid;
	private NetInfo initNetInfo;

	public SystemMonitor() {
		kmsPid = getKmsPid();
	}

	public static void main(String[] args)
			throws InterruptedException, IOException {

		int monitorPort = args.length > 0 ? Integer.parseInt(args[0])
				: MONITOR_PORT_DEFAULT;
		final SystemMonitor monitor = new SystemMonitor();

		ServerSocket server = new ServerSocket(monitorPort);
		System.out.println("Waiting for incoming messages...");
		boolean run = true;

		while (run) {
			Socket socket = server.accept();
			Object result = null;
			BufferedReader input = null;
			ObjectOutputStream output = null;

			try {
				output = new ObjectOutputStream(socket.getOutputStream());
				input = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));

				String message = input.readLine();

				if (message != null) {
					System.out.println("Message received " + message);
					String[] commands = message.split(" ");
					switch (commands[0]) {
					case "measureKms":
						result = monitor.measureKms();
						break;
					case "destroy":
						result = "Stopping KMS monitor";
						run = false;
						break;
					default:
						result = ERR + "Invalid command: " + message;
						break;
					}

					System.out.println("Sending back message " + result);
					output.writeObject(result);
				}

				output.close();
				input.close();
				socket.close();

			} catch (IOException e) {
				result = ERR + e.getMessage();
				e.printStackTrace();
			}
		}
		server.close();

	}

	public KmsSystemInfo measureKms() {
		KmsSystemInfo info = new KmsSystemInfo();

		// Bandwidth (bytes tx and rx)
		if (initNetInfo == null) {
			initNetInfo = getInitNetInfo();
		}
		NetInfo newNetInfo = getNetInfo(initNetInfo);
		info.setNetInfo(newNetInfo);

		// CPU usage (%)
		info.setCpuPercent(getCpuUsage());

		// Memory and swap usage (bytes)
		double[] mem = getMemSwap();
		info.setMem((long) mem[0]);
		info.setSwap((long) mem[1]);
		info.setMemPercent(mem[2]);
		info.setSwapPercent(mem[3]);

		// Number of threads
		info.setNumThreadsKms(getNumThreads(kmsPid));

		return info;
	}

	public NetInfo getNetInfo(NetInfo initNetInfo, NetInfo lastNetInfo) {
		NetInfo netInfo = new NetInfo();
		String out = runAndWait("/bin/sh", "-c",
				"cat /proc/net/dev | awk 'NR > 2'");

		String[] lines = out.split("\n");
		for (String line : lines) {
			String[] split = line.trim().replaceAll(" +", " ").split(" ");
			String iface = split[0].replace(":", "");
			long rxBytes = Long.parseLong(split[1]);
			long txBytes = Long.parseLong(split[9]);
			netInfo.putNetInfo(iface, rxBytes, txBytes);
		}
		if (initNetInfo != null) {
			netInfo.decrementInitInfo(initNetInfo);
		}
		if (lastNetInfo != null) {
			netInfo.decrementInitInfo(lastNetInfo);
		}
		return netInfo;
	}

	public NetInfo getNetInfo(NetInfo initNetInfo) {
		return getNetInfo(initNetInfo, null);
	}

	public NetInfo getInitNetInfo() {
		return getNetInfo(null, null);
	}

	public double getCpuUsage() {
		String[] cpu = runAndWait("/bin/sh", "-c",
				"cat /proc/stat | grep '^cpu ' | awk '{print substr($0, index($0, $2))}'")
						.replaceAll("\n", "").split(" ");

		double idle = Double.parseDouble(cpu[3]);
		double total = 0;
		for (String s : cpu) {
			total += Double.parseDouble(s);
		}
		double diffIdle = idle - prevIdle;
		double diffTotal = total - prevTotal;
		double diffUsage = (1000 * (diffTotal - diffIdle) / diffTotal + 5) / 10;

		prevTotal = total;
		prevIdle = idle;

		return diffUsage;
	}

	public double[] getMemSwap() {
		String[] mem = runAndWait("free").replaceAll("\n", ",")
				.replaceAll(" +", " ").split(" ");

		long usedMem = Long.parseLong(mem[15]);
		long usedSwap = Long.parseLong(mem[19]);
		long totalMem = Long.parseLong(mem[7]);
		long totalSwap = Long.parseLong(mem[20]);

		double percetageMem = ((double) usedMem / (double) totalMem) * 100;
		double percetageSwap = ((double) usedSwap / (double) totalSwap) * 100;

		if (Double.isNaN(percetageMem)) {
			percetageMem = 0;
		}
		if (Double.isNaN(percetageSwap)) {
			percetageSwap = 0;
		}

		double[] out = { usedMem, usedSwap, percetageMem, percetageSwap };
		return out;
	}

	private int getKmsPid() {
		System.out.println("Looking for KMS process...");

		boolean reachable = false;
		long endTimeMillis = System.currentTimeMillis()
				+ (KMS_WAIT_TIMEOUT * 1000);

		String kmsPid;
		while (true) {
			kmsPid = runAndWait("/bin/sh", "-c",
					"ps axf | grep kurento-media-server | grep -v grep | awk '{print $1}'")
							.replaceAll("\n", "");
			reachable = !kmsPid.equals("");
			if (kmsPid.contains(" ")) {
				throw new RuntimeException(
						"More than one KMS process are started (PIDs:" + kmsPid
								+ ")");
			}
			if (reachable) {
				break;
			}

			// Poll time to wait host (1 second)
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			if (System.currentTimeMillis() > endTimeMillis) {
				break;
			}
		}
		if (!reachable) {
			throw new RuntimeException(
					"KMS is not started in the local machine");
		}

		System.out.println(
				"KMS process located in local machine with PID " + kmsPid);
		return Integer.parseInt(kmsPid);
	}

	private int getNumThreads(int kmsPid) {
		return Integer.parseInt(runAndWait("/bin/sh", "-c",
				"cat /proc/" + kmsPid + "/stat | awk '{print $20}'")
						.replaceAll("\n", ""));
	}

	private String runAndWait(final String... command) {
		Process p;
		try {
			p = new ProcessBuilder(command).redirectErrorStream(true).start();

			return inputStreamToString(p.getInputStream());

		} catch (IOException e) {
			throw new RuntimeException(
					"Exception executing command on the shell: "
							+ Arrays.toString(command),
					e);
		}
	}

	private String inputStreamToString(InputStream in) throws IOException {
		InputStreamReader is = new InputStreamReader(in);
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(is);
		String read = br.readLine();

		while (read != null) {
			sb.append(read);
			read = br.readLine();
			sb.append('\n');
			sb.append(' ');
		}

		return sb.toString().trim();
	}

}
