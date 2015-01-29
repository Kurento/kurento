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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kurento.test.client.BrowserClient;

/**
 * System monitor class, used to check the CPU usage, memory, swap, and network
 * of the machine running the tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class SystemMonitor {

	private Thread thread;
	private Map<Long, SystemInfo> infoMap;
	private long samplingTime = 100; // Default sampling time, in milliseconds
	private double prevTotal = 0;
	private double prevIdle = 0;
	private int numClients = 0;
	private double currentLatency = 0;
	private int latencyHints = 0;
	private int latencyErrors = 0;
	private List<BrowserClient> browserList;

	private final static String OK = "ok";
	private final static String ERR = "error: ";

	public static final String MONITOR_PORT_PROP = "monitor.port";
	public static final int MONITOR_PORT_DEFAULT = 12345;
	public static final String OUTPUT_CSV = "/kms-monitor.csv";

	public SystemMonitor() {
		infoMap = Collections
				.synchronizedSortedMap(new TreeMap<Long, SystemInfo>());
	}

	public SystemMonitor(long samplingTime) {
		this();
		this.samplingTime = samplingTime;
	}

	public static void main(String[] args) throws InterruptedException,
			IOException {

		int monitorPort = args.length > 0 ? Integer.parseInt(args[0])
				: MONITOR_PORT_DEFAULT;
		final SystemMonitor monitor = new SystemMonitor();

		ServerSocket server = new ServerSocket(monitorPort);
		System.out.println("Waiting for incoming messages...");
		boolean run = true;

		while (run) {
			final Socket socket = server.accept();

			String result = OK;

			PrintWriter output = null;
			BufferedReader input = null;
			try {
				output = new PrintWriter(socket.getOutputStream(), true);
				input = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				String message = input.readLine();
				System.out.println("Message received " + message);

				if (message != null) {
					String[] commands = message.split(" ");
					switch (commands[0]) {
					case "start":
						monitor.start();
						break;
					case "stop":
						monitor.stop();
						break;
					case "destroy":
						run = false;
						break;
					case "writeResults":
						monitor.writeResults(commands[1] + OUTPUT_CSV);
						break;
					case "incrementNumClients":
						monitor.incrementNumClients();
						break;
					case "decrementNumClients":
						monitor.decrementNumClients();
						break;
					case "addCurrentLatency":
						monitor.addCurrentLatency(Double
								.parseDouble(commands[1]));
						break;
					case "setSamplingTime":
						monitor.setSamplingTime(Long.parseLong(commands[1]));
						break;
					case "incrementLatencyErrors":
						monitor.incrementLatencyErrors();
						break;
					default:
						result = ERR + "Invalid command: " + message;
						break;
					}
					System.out.println("Sending back message " + result);
					output.println(result);
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

	public void start() {
		final long start = new Date().getTime();
		final NetInfo initNetInfo = getInitNetInfo();
		final int kmsPid = getKmsPid();

		thread = new Thread() {
			@Override
			public void run() {
				// NetInfo lastNetInfo = null;
				while (true) {
					SystemInfo info = new SystemInfo();

					// Bandwidth (bytes tx and rx)
					NetInfo newNetInfo = getNetInfo(initNetInfo);
					info.setNetInfo(newNetInfo);
					// lastNetInfo = newNetInfo;

					// CPU usage (%)
					info.setCpuPercent(getCpuUsage());

					// Memory and swap usage (bytes)
					double[] mem = getMemSwap();
					info.setMem((long) mem[0]);
					info.setSwap((long) mem[1]);
					info.setMemPercent(mem[2]);
					info.setSwapPercent(mem[3]);

					// Number of clients
					info.setNumClients(numClients);

					// Latency
					info.setLatency(getLatency());
					info.setLatencyErrors(latencyErrors);

					// Number of threads
					info.setNumThreadsKms(getNumThreads(kmsPid));

					// Browser Statistics
					if (browserList != null) {
						for (BrowserClient bc : browserList) {
							Map<String, Object> rtc = bc.getRtcStats();
							info.addRtcStats(rtc);
						}
					}

					infoMap.put(new Date().getTime() - start, info);

					try {
						Thread.sleep(samplingTime);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		thread.interrupt();
		thread.stop();
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

	public void writeResults(String csvTitle) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(csvTitle));
		boolean header = false;
		String emptyStats = "";
		List<String> rtcHeader = null;

		for (long time : infoMap.keySet()) {
			if (!header) {
				pw.print("time, cpu_percetage, mem_bytes, mem_percentage, swap_bytes, swap_percentage, clients_number, kms_threads_number, latency_ms_avg, latency_errors_number"
						+ infoMap.get(time).getNetInfo().parseHeaderEntry());

				// Browser statistics. First entries may be empty, so we have to
				// iterate to find values in the statistics in order to write
				// the header in the resulting CSV
				if (browserList != null) {
					rtcHeader = new ArrayList<>();
					for (SystemInfo info : infoMap.values()) {
						if (info.getRtcStats() != null
								&& !info.getRtcStats().isEmpty()) {
							for (String rtcStatsKey : info.getRtcStats()
									.keySet()) {
								if (!rtcHeader.contains(rtcStatsKey)) {
									rtcHeader.add(rtcStatsKey);
									pw.print(", "
											+ rtcStatsKey
											+ StatsOperation.map().get(
													rtcStatsKey));
									emptyStats += ",";
								}
							}
						}
					}
				}

				pw.println("");
				header = true;
			}
			String parsedtime = new SimpleDateFormat("mm:ss.SSS").format(time);
			double cpu = infoMap.get(time).getCpuPercent();
			long mem = infoMap.get(time).getMem();
			double memPercent = infoMap.get(time).getMemPercent();
			long swap = infoMap.get(time).getSwap();
			double swapPercent = infoMap.get(time).getSwapPercent();

			pw.print(parsedtime + "," + cpu + "," + mem + "," + memPercent
					+ "," + swap + "," + swapPercent + ","
					+ infoMap.get(time).getNumClients() + ","
					+ infoMap.get(time).getNumThreadsKms() + ","
					+ infoMap.get(time).getLatency() + ","
					+ infoMap.get(time).getLatencyErrors()
					+ infoMap.get(time).getNetInfo().parseNetEntry());

			// Browser statistics
			if (browserList != null) {
				if (infoMap.get(time).getRtcStats() != null
						&& !infoMap.get(time).getRtcStats().isEmpty()) {
					for (String key : rtcHeader) {
						pw.print(",");
						if (infoMap.get(time).getRtcStats().containsKey(key)) {
							pw.print(infoMap.get(time).getRtcStats().get(key));
						}
					}
				} else {
					pw.print(emptyStats);
				}
			}

			pw.println("");
		}
		pw.close();
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

		double[] out = { usedMem, usedSwap, percetageMem, percetageSwap };
		return out;
	}

	private int getKmsPid() {
		String kmsPid = runAndWait("/bin/sh", "-c",
				"ps axf | grep kurento-media-server | grep -v grep | awk '{print $1}'")
				.replaceAll("\n", "");
		if (kmsPid.equals("")) {
			throw new RuntimeException("KMS is not started");
		} else if (kmsPid.contains(" ")) {
			throw new RuntimeException(
					"More than one KMS process are started (PIDs:" + kmsPid
							+ ")");
		}
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
							+ Arrays.toString(command), e);
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

	public int getNumClients() {
		return numClients;
	}

	public synchronized void incrementNumClients() {
		this.numClients++;
	}

	public synchronized void decrementNumClients() {
		this.numClients--;
	}

	public int getLatencyErrors() {
		return latencyErrors;
	}

	public synchronized void incrementLatencyErrors() {
		this.latencyErrors++;
	}

	public double getLatency() {
		double latency = (latencyHints > 0) ? currentLatency / latencyHints : 0;
		this.currentLatency = 0;
		this.latencyHints = 0;
		return latency;
	}

	public synchronized void addCurrentLatency(double currentLatency) {
		this.currentLatency += currentLatency;
		this.latencyHints++;
	}

	public void setSamplingTime(long samplingTime) {
		this.samplingTime = samplingTime;
	}

	public void addBrowser(BrowserClient browser) {
		if (browserList == null) {
			browserList = new CopyOnWriteArrayList<>();
		}
		browserList.add(browser);
	}

}
