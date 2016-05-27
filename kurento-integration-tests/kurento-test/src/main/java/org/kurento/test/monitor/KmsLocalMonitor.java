/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * System monitor class, used to check the CPU usage, memory, swap, and network of the machine
 * running the tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KmsLocalMonitor extends KmsMonitor {

  public static final String MONITOR_PORT_PROP = "kms.monitor.port";
  public static final int MONITOR_PORT_DEFAULT = 12345;
  public static final int KMS_WAIT_TIMEOUT = 10; // seconds

  private static final String ERR = "error: ";

  private double prevTotal = 0;
  private double prevIdle = 0;
  private int kmsPid;
  private NetInfo initNetInfo;

  public KmsLocalMonitor() {
    kmsPid = getKmsPid();
  }

  public static void main(String[] args) throws InterruptedException, IOException {

    int monitorPort = args.length > 0 ? Integer.parseInt(args[0]) : MONITOR_PORT_DEFAULT;
    final KmsLocalMonitor monitor = new KmsLocalMonitor();

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
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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

  @Override
  protected NetInfo getNetInfo() {
    NetInfo netInfo = new NetInfo();
    String out = runAndWait("/bin/sh", "-c", "cat /proc/net/dev | awk 'NR > 2'");

    String[] lines = out.split("\n");
    for (String line : lines) {
      String[] split = line.trim().replaceAll(" +", " ").split(" ");
      String iface = split[0].replace(":", "");
      long rxBytes = Long.parseLong(split[1]);
      long txBytes = Long.parseLong(split[9]);
      netInfo.putNetInfo(iface, rxBytes, txBytes);
    }
    if (initNetInfo == null) {
      initNetInfo = netInfo;
    }
    netInfo.decrementInitInfo(initNetInfo);
    return netInfo;
  }

  @Override
  protected double getCpuUsage() {
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

  @Override
  protected double[] getMem() {
    String[] mem = runAndWait("free").replaceAll("\n", ",").replaceAll(" +", " ").split(" ");

    long usedMem = Long.parseLong(mem[15]);
    long totalMem = Long.parseLong(mem[7]);

    double percetageMem = (double) usedMem / (double) totalMem * 100;

    if (Double.isNaN(percetageMem)) {
      percetageMem = 0;
    }

    double[] out = { usedMem, percetageMem };
    return out;
  }

  @Override
  protected int getKmsPid() {
    System.out.println("Looking for KMS process...");

    boolean reachable = false;
    long endTimeMillis = System.currentTimeMillis() + KMS_WAIT_TIMEOUT * 1000;

    String kmsPid;
    while (true) {
      kmsPid = runAndWait("/bin/sh", "-c",
          "ps axf | grep /usr/bin/kurento-media-server | grep -v grep | awk '{print $1}'")
              .replaceAll("\n", "");
      reachable = !kmsPid.equals("");
      if (kmsPid.contains(" ")) {
        throw new RuntimeException("More than one KMS process are started (PIDs:" + kmsPid + ")");
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
      throw new RuntimeException("KMS is not started in the local machine");
    }

    System.out.println("KMS process located in local machine with PID " + kmsPid);
    return Integer.parseInt(kmsPid);
  }

  @Override
  protected int getNumThreads() {
    return Integer
        .parseInt(runAndWait("/bin/sh", "-c", "cat /proc/" + kmsPid + "/stat | awk '{print $20}'")
            .replaceAll("\n", ""));
  }

  private String runAndWait(final String... command) {
    Process p;
    try {
      p = new ProcessBuilder(command).redirectErrorStream(true).start();

      return inputStreamToString(p.getInputStream());

    } catch (IOException e) {
      throw new RuntimeException(
          "Exception executing command on the shell: " + Arrays.toString(command), e);
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
