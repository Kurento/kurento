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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.kurento.test.Shell;

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

	public SystemMonitor() {
		infoMap = Collections
				.synchronizedSortedMap(new TreeMap<Long, SystemInfo>());
	}

	public SystemMonitor(long samplingTime) {
		this();
		this.samplingTime = samplingTime;
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
		String out = Shell.runAndWaitNoLog("/bin/sh", "-c",
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
		for (long time : infoMap.keySet()) {
			if (!header) {
				pw.println("time, cpu_percetage, mem_bytes, mem_percentage, swap_bytes, swap_percentage, clients_number, kms_threads_number, latency_ms, latency_errors_number"
						+ infoMap.get(time).getNetInfo().parseHeaderEntry());
				header = true;
			}
			String parsedtime = new SimpleDateFormat("mm:ss.SSS").format(time);
			double cpu = infoMap.get(time).getCpuPercent();
			long mem = infoMap.get(time).getMem();
			double memPercent = infoMap.get(time).getMemPercent();
			long swap = infoMap.get(time).getSwap();
			double swapPercent = infoMap.get(time).getSwapPercent();

			pw.println(parsedtime + "," + cpu + "," + mem + "," + memPercent
					+ "," + swap + "," + swapPercent + ","
					+ infoMap.get(time).getNumClients() + ","
					+ infoMap.get(time).getNumThreadsKms() + ","
					+ infoMap.get(time).getLatency() + ","
					+ infoMap.get(time).getLatencyErrors()
					+ infoMap.get(time).getNetInfo().parseNetEntry());
		}
		pw.close();
	}

	public double getCpuUsage() {
		String[] cpu = Shell
				.runAndWaitNoLog("/bin/sh", "-c",
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
		String[] mem = Shell.runAndWaitNoLog("free").replaceAll("\n", " ")
				.replaceAll(" +", " ").split(" ");

		long usedMem = Long.parseLong(mem[16]);
		long usedSwap = Long.parseLong(mem[20]);
		long totalMem = Long.parseLong(mem[8]);
		long totalSwap = Long.parseLong(mem[21]);

		double percetageMem = ((double) usedMem / (double) totalMem) * 100;
		double percetageSwap = ((double) usedSwap / (double) totalSwap) * 100;

		double[] out = { usedMem, usedSwap, percetageMem, percetageSwap };
		return out;
	}

	private int getKmsPid() {
		// TODO improve this
		// Shell.runAndWait("sh", "-c", "kill `cat " + workspace + "kms-pid`");
		return Integer
				.parseInt(Shell
						.runAndWaitNoLog("/bin/sh", "-c",
								"ps axf | grep kurento-media-server | grep -v grep | awk '{print $1}'")
						.replaceAll("\n", ""));
	}

	private int getNumThreads(int kmsPid) {
		return Integer.parseInt(Shell.runAndWaitNoLog("/bin/sh", "-c",
				"cat /proc/" + kmsPid + "/stat | awk '{print $20}'")
				.replaceAll("\n", ""));
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

}
