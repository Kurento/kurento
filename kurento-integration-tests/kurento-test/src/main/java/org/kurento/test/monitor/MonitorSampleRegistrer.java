package org.kurento.test.monitor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class MonitorSampleRegistrer {

  private Map<Long, MonitorSample> samples = new TreeMap<>();

  private boolean showLantency = false;

  public void addSample(long time, MonitorSample sample) {
    samples.put(time, sample);
  }

  public void writeResults(String csvFile) throws IOException {

    Path path = Paths.get(csvFile);
    if (!Files.exists(path.getParent())) {
      Files.createDirectories(path.getParent());
    }

    try (PrintWriter pw = new PrintWriter(new FileWriter(csvFile))) {

      printKmsProcessHeaders(pw);
      Map<String, List<String>> headers = printWebRtcHeaders(pw);

      pw.println("");

      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss.SSS");

      for (long time : samples.keySet()) {

        MonitorSample sample = samples.get(time);

        pw.print(simpleDateFormat.format(time) + ",");

        printKmsProcessStats(pw, sample);
        printWebRtcStats(pw, headers, sample);

        pw.println("");

      }
    }
  }

  private void printWebRtcStats(PrintWriter pw, Map<String, List<String>> headers,
      MonitorSample sample) {

    for (Entry<String, List<String>> e : headers.entrySet()) {

      WebRtcStats stats = sample.getWebRtcStats(e.getKey());

      if (stats != null) {
        for (Object value : stats.calculateValues(e.getValue())) {
          if (value != null) {
            pw.print("," + value);
          } else {
            pw.print(",");
          }
        }
      } else {
        for (int i = 0; i < e.getValue().size(); i++) {
          pw.print(",");
        }
      }
    }
  }

  private void printKmsProcessHeaders(PrintWriter pw) {
    pw.print("time,clients_number,kms_threads_number");
    pw.print(",cpu_percetage,mem_bytes,mem_percentage");

    if (showLantency) {
      pw.print(",latency_ms_avg,latency_errors_number");
    }

    MonitorSample firstSample = samples.entrySet().iterator().next().getValue();

    pw.print(firstSample.getSystemInfo().getNetInfo().createHeader());

  }

  private void printKmsProcessStats(PrintWriter pw, MonitorSample sample) {

    KmsSystemInfo systemInfo = sample.getSystemInfo();

    int numClients = sample.getNumClients();
    int numThreadsKms = systemInfo.getNumThreadsKms();
    double cpu = systemInfo.getCpuPercent();
    long mem = systemInfo.getMem();
    double memPercent = systemInfo.getMemPercent();

    pw.format(Locale.ENGLISH, numClients + "," + numThreadsKms + ",%.2f," + mem, cpu, memPercent);

    if (showLantency) {
      pw.print("," + sample.getLatency() + "," + sample.getLatencyErrors());
    }

    pw.print(systemInfo.getNetInfo().createEntries());
  }

  private Map<String, List<String>> printWebRtcHeaders(PrintWriter pw) {

    Map<String, List<String>> headers = new TreeMap<>();

    for (MonitorSample sample : samples.values()) {

      Map<String, WebRtcStats> statsMap = sample.getStats();

      for (Entry<String, WebRtcStats> stats : statsMap.entrySet()) {
        List<String> prevHeaders = headers.get(stats.getKey());
        List<String> newHeaders = stats.getValue().calculateHeaders();
        if (prevHeaders == null) {
          headers.put(stats.getKey(), newHeaders);
        } else {
          for (String newHeader : newHeaders) {
            if (!prevHeaders.contains(newHeader)) {
              prevHeaders.add(newHeader);
            }
          }
        }
      }
    }

    for (Entry<String, List<String>> bHeaders : headers.entrySet()) {
      for (String h : bHeaders.getValue()) {
        pw.print("," + bHeaders.getKey() + "_" + h);
      }
    }

    return headers;
  }

}
