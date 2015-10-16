package org.kurento.test.monitor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.kurento.client.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorSampleRegistrer {

	private static final Logger log = LoggerFactory
			.getLogger(MonitorSampleRegistrer.class);

	private Map<Long, MonitorSample> infoMap = new TreeMap<>();

	private NumberFormat formatter = new DecimalFormat("#0.00");

	private boolean showLantency = false;

	public void addSample(long time, MonitorSample sample) {
		infoMap.put(time, sample);
	}

	public void writeResults(String csvFile) throws IOException {

		try (PrintWriter pw = new PrintWriter(new FileWriter(csvFile))) {

			String emptyStats = "";
			List<String> rtcClientHeader = new ArrayList<>();
			List<String> rtcServerHeader = new ArrayList<>();

			emptyStats = printHeader(pw, emptyStats, rtcClientHeader,
					rtcServerHeader);

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
					"mm:ss.SSS");

			for (long time : infoMap.keySet()) {

				MonitorSample sample = infoMap.get(time);

				pw.print(simpleDateFormat.format(time) + ",");

				printKmsProcessStats(pw, sample);
				printPeerConnectionStats(pw, emptyStats, rtcClientHeader,
						sample);
				printWebRtcEndpointStats(pw, emptyStats, rtcServerHeader,
						sample);

				pw.println("");

			}
		}
	}

	private void printKmsProcessStats(PrintWriter pw, MonitorSample sample) {

		KmsSystemInfo systemInfo = sample.getSystemInfo();

		double cpu = systemInfo.getCpuPercent();
		long mem = systemInfo.getMem();
		double memPercent = systemInfo.getMemPercent();
		long swap = systemInfo.getSwap();
		double swapPercent = systemInfo.getSwapPercent();

		pw.print(sample.getNumClients() + "," + systemInfo.getNumThreadsKms()
				+ "," + formatter.format(cpu) + "," + mem + ","
				+ formatter.format(memPercent) + "," + swap + ","
				+ formatter.format(swapPercent));

		if (showLantency) {
			pw.print("," + sample.getLatency() + ","
					+ sample.getLatencyErrors());
		}

		pw.print(systemInfo.getNetInfo().parseNetEntry());
	}

	private String printHeader(PrintWriter pw, String emptyStats,
			List<String> rtcClientHeader, List<String> rtcServerHeader) {

		pw.print("time,clients_number,kms_threads_number");
		pw.print(",cpu_percetage,mem_bytes,mem_percentage");
		pw.print(",swap_bytes,swap_percentage");

		if (showLantency) {
			pw.print(",latency_ms_avg,latency_errors_number");
		}

		MonitorSample firstSample = infoMap.entrySet().iterator().next()
				.getValue();

		pw.print(firstSample.getSystemInfo().getNetInfo().parseHeaderEntry());

		for (MonitorSample info : infoMap.values()) {
			Map<String, Object> clientRtcStats = info.getClientRtcStats();
			if (clientRtcStats != null && !clientRtcStats.isEmpty()) {
				for (String rtcStatsKey : clientRtcStats.keySet()) {
					if (!rtcClientHeader.contains(rtcStatsKey)) {
						rtcClientHeader.add(rtcStatsKey);
						pw.print("," + rtcStatsKey);
						emptyStats += ",";
					}
				}
			}
		}

		for (MonitorSample info : infoMap.values()) {
			Map<String, Stats> serverRtcStats = info.getServerRtcStats();
			if (serverRtcStats != null && !serverRtcStats.isEmpty()) {
				for (String rtcStatsKey : serverRtcStats.keySet()) {
					Object object = serverRtcStats.get(rtcStatsKey);
					for (Method method : object.getClass().getMethods()) {
						if (isGetter(method)) {
							String keyList = rtcStatsKey + "_"
									+ getGetterName(method);
							if (!rtcServerHeader.contains(keyList)) {
								rtcServerHeader.add(keyList);
								pw.print("," + keyList);
								emptyStats += ",";
							}
						}
					}
				}
			}
		}

		pw.println("");
		return emptyStats;
	}

	private void printWebRtcEndpointStats(PrintWriter pw, String emptyStats,
			List<String> rtcServerHeader, MonitorSample sample) {

		Map<String, Stats> serverRtcStats = sample.getServerRtcStats();

		if (serverRtcStats != null) {

			Map<String, Object> rtcServerStatsValues = new HashMap<>();

			if (serverRtcStats != null && !serverRtcStats.isEmpty()) {
				for (String rtcStatsKey : serverRtcStats.keySet()) {
					Object object = serverRtcStats.get(rtcStatsKey);
					for (Method method : object.getClass().getMethods()) {
						if (isGetter(method)) {
							Object value = null;
							try {
								value = method.invoke(object);
							} catch (Exception e) {
								log.error("Exception invoking method", e);
							}

							String keyList = rtcStatsKey + "_"
									+ getGetterName(method);
							rtcServerStatsValues.put(keyList, value);
						}
					}
				}

				for (String rtcHeader : rtcServerHeader) {
					pw.print(",");
					if (rtcServerStatsValues.get(rtcHeader) != null) {
						pw.print(rtcServerStatsValues.get(rtcHeader));
					}
				}

			}
		} else {
			pw.print(emptyStats);
		}
	}

	private void printPeerConnectionStats(PrintWriter pw, String emptyStats,
			List<String> rtcClientHeader, MonitorSample sample) {

		Map<String, Object> clientRtcStats = sample.getClientRtcStats();
		if (clientRtcStats != null) {
			if (clientRtcStats != null && !clientRtcStats.isEmpty()) {
				for (String key : rtcClientHeader) {
					pw.print(",");
					if (clientRtcStats.containsKey(key)) {
						pw.print(clientRtcStats.get(key));
					}
				}
			} else {
				pw.print(emptyStats);
			}
		}
	}

	private boolean isGetter(Method method) {
		return (method.getName().startsWith("get")
				|| method.getName().startsWith("is"))
				&& !method.getName().equals("getClass");
	}

	private String getGetterName(Method method) {
		String name = method.getName();
		if (name.startsWith("get")) {
			name = name.substring(3);
		} else if (name.startsWith("is")) {
			name = name.substring(2);
		}
		return name;
	}

}
