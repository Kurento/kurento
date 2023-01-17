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

package org.kurento.test.base;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixReadMem;
import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_URL_TIMEOUT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_URL_TIMEOUT_PROPERTY;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;
import org.kurento.test.internal.AbortableCountDownLatch;
import org.kurento.test.lifecycle.FailedTest;
import org.kurento.test.utils.Shell;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * Base for Kurento tests that use browsers.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public abstract class BrowserTest<W extends WebPage> extends KurentoTest {

  public static Logger log = LoggerFactory.getLogger(BrowserTest.class);
  public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);
  public static final int OCR_TIME_THRESHOLD_MS = 300;
  public static final int OCR_COLOR_THRESHOLD = 180;
  public static final String LATENCY_KEY = "E2ELatencyMs";
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("H:mm:ss:S");
  public static final double FPS = 30;
  public static final int BLOCKSIZE = 1;
  public static final String SSIM_KEY = "avg_ssim";
  public static final String PSNR_KEY = "avg_psnr";
  public static final String PNG = ".png";
  public static final String Y4M = ".y4m";

  private static Map<String, LogEntries> browserLogs = new ConcurrentHashMap<>();

  private final Map<String, W> pages = new ConcurrentHashMap<>();

  @Before
  public void setupBrowserTest() throws InterruptedException {
    if (testScenario != null && testScenario.getBrowserMap() != null
        && testScenario.getBrowserMap().size() > 0) {
      ExecutorService executor = Executors.newFixedThreadPool(testScenario.getBrowserMap().size());
      final AbortableCountDownLatch latch = new AbortableCountDownLatch(
          testScenario.getBrowserMap().size());
      for (final String browserKey : testScenario.getBrowserMap().keySet()) {

        executor.execute(new Runnable() {

          @Override
          public void run() {
            try {
              Browser browser = testScenario.getBrowserMap().get(browserKey);

              int timeout = getProperty(TEST_URL_TIMEOUT_PROPERTY, TEST_URL_TIMEOUT_DEFAULT);

              URL url = browser.getUrl();
              if (!testScenario.getUrlList().contains(url)) {
                waitForHostIsReachable(url, timeout);
                testScenario.getUrlList().add(url);
              }
              initBrowser(browserKey, browser);
              latch.countDown();
            } catch (Throwable t) {
              latch.abort("Exception setting up test. A browser could not be initialised", t);
              t.printStackTrace();
            }
          }
        });
      }

      latch.await();
    }
  }

  private void initBrowser(String browserKey, Browser browser) throws IOException {
    browser.setId(browserKey);
    browser.setName(getTestMethodName());
    browser.init();
    browser.injectKurentoTestJs();
  }

  @After
  public void teardownBrowserTest() {
    if (testScenario != null) {
      for (Browser browser : testScenario.getBrowserMap().values()) {
        String browserId = browser.getId();
        try {
          WebDriver webDriver = browser.getWebDriver();
          if (webDriver != null) {
            String screenshotFileName = getDefaultOutputFile("-" + browserId + ".png");
            getOrCreatePage(browserId).takeScreeshot(screenshotFileName);
            browserLogs.put(browserId,
                webDriver.manage().logs().get(LogType.BROWSER));
          } else {
            log.warn("It was not possible to recover logs for {} "
                + "since browser is no longer available (maybe "
                + "it has been closed manually or crashed)", browserId);
          }
        } catch (Exception e) {
          log.warn("Exception getting logs {}", browserId, e);
        }
        try {
          browser.close();
        } catch (Exception e) {
          log.warn("Exception closing browser {}", browserId, e);
        }
      }
    }
  }

  @FailedTest
  public static void storeBrowsersLogs() {
    List<String> lines = new ArrayList<>();
    for (String browserKey : browserLogs.keySet()) {
      for (LogEntry logEntry : browserLogs.get(browserKey)) {
        lines.add(logEntry.toString());
      }

      File file = new File(getDefaultOutputFile("-" + browserKey + "-console.log"));

      try {
        FileUtils.writeLines(file, lines);
      } catch (IOException e) {
        log.error("Error while writing browser log to a file", e);
      }
    }
  }

  public TestScenario getTestScenario() {
    return testScenario;
  }

  public void addBrowser(String browserKey, Browser browser) throws IOException {
    testScenario.getBrowserMap().put(browserKey, browser);
    initBrowser(browserKey, browser);
  }

  public W getPage(String browserKey) {
    return assertAndGetPage(browserKey);
  }

  public W getPage() {
    try {
      return assertAndGetPage(BrowserConfig.BROWSER);

    } catch (RuntimeException e) {
      if (testScenario.getBrowserMap().isEmpty()) {
        throw new RuntimeException("Empty test scenario: no available browser to run tests!");
      } else {
        String browserKey = testScenario.getBrowserMap().entrySet().iterator().next().getKey();
        log.debug(BrowserConfig.BROWSER + " is not registered in test scenarario, instead"
            + " using first browser in the test scenario, i.e. " + browserKey);

        return getOrCreatePage(browserKey);
      }
    }
  }

  public W getPage(int index) {
    return assertAndGetPage(BrowserConfig.BROWSER + index);
  }

  public W getPresenter() {
    return assertAndGetPage(BrowserConfig.PRESENTER);
  }

  public W getPresenter(int index) {
    return assertAndGetPage(BrowserConfig.PRESENTER + index);
  }

  public W getViewer() {
    return assertAndGetPage(BrowserConfig.VIEWER);
  }

  public W getViewer(int index) {
    return assertAndGetPage(BrowserConfig.VIEWER + index);
  }

  private W assertAndGetPage(String browserKey) {
    if (!testScenario.getBrowserMap().keySet().contains(browserKey)) {
      throw new RuntimeException(browserKey + " is not registered as browser in the test scenario");
    }
    return getOrCreatePage(browserKey);
  }

  private synchronized W getOrCreatePage(String browserKey) {
    W webPage;
    if (pages.containsKey(browserKey)) {
      webPage = pages.get(browserKey);
      webPage.setBrowser(testScenario.getBrowserMap().get(browserKey));
    } else {
      webPage = createWebPage();
      webPage.setBrowser(testScenario.getBrowserMap().get(browserKey));
      pages.put(browserKey, webPage);
    }

    return webPage;
  }

  @SuppressWarnings("unchecked")
  protected W createWebPage() {

    Class<?> testClientClass = getParamType(this.getClass());

    try {
      return (W) testClientClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(
          "Exception creating an instance of class " + testClientClass.getName(), e);
    }
  }

  public static Class<?> getParamType(Class<?> testClass) {

    Type genericSuperclass = testClass.getGenericSuperclass();

    if (genericSuperclass != null) {

      if (genericSuperclass instanceof Class) {
        return getParamType((Class<?>) genericSuperclass);
      }

      ParameterizedType paramClass = (ParameterizedType) genericSuperclass;

      return (Class<?>) paramClass.getActualTypeArguments()[0];
    }

    throw new RuntimeException("Unable to obtain the type paramter of KurentoTest");
  }

  public void waitForHostIsReachable(URL url, int timeout) {
    long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.SECONDS);
    long endTimeMillis = System.currentTimeMillis() + timeoutMillis;

    log.debug("Waiting for {} to be reachable (timeout {} seconds)", url, timeout);

    try {
      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      } };

      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      HostnameVerifier allHostsValid = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

      int responseCode = 0;
      while (true) {
        try {
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          connection.setConnectTimeout((int) timeoutMillis);
          connection.setReadTimeout((int) timeoutMillis);
          connection.setRequestMethod("HEAD");
          responseCode = connection.getResponseCode();

          break;
        } catch (SSLHandshakeException | SocketException e) {
          log.warn("Error {} waiting URL {}, trying again in 1 second", e.getMessage(), url);
          // Polling to wait a consistent SSL state
          Thread.sleep(1000);
        }
        if (System.currentTimeMillis() > endTimeMillis) {
          break;
        }
      }

      if (responseCode != HttpURLConnection.HTTP_OK) {
        log.warn("URL " + url + " not reachable. Response code=" + responseCode);
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("URL " + url + " not reachable in " + timeout + " seconds ("
          + e.getClass().getName() + ", " + e.getMessage() + ")");
    }

    log.debug("URL {} already reachable", url);
  }

  public void waitSeconds(long waitTime) {
    waitMilliSeconds(TimeUnit.SECONDS.toMillis(waitTime));
  }

  public void waitMilliSeconds(long waitTime) {
    try {
      Thread.sleep(waitTime);
    } catch (InterruptedException e) {
      log.warn("InterruptedException waiting {} milliseconds", waitTime, e);
    }
  }

  public void syncTimeForOcr(final W[] webpages, final String[] videoTagsId,
      final String[] peerConnectionsId) throws InterruptedException {
    int webpagesLength = webpages.length;
    int videoTagsLength = videoTagsId.length;
    if (webpagesLength != videoTagsLength) {
      throw new KurentoException("The size of webpage arrays (" + webpagesLength
          + "}) must be the same as videoTags (" + videoTagsLength + ")");
    }

    final ExecutorService service = Executors.newFixedThreadPool(webpagesLength);
    final CountDownLatch latch = new CountDownLatch(webpagesLength);

    for (int i = 0; i < webpagesLength; i++) {
      final int j = i;
      service.execute(new Runnable() {
        @Override
        public void run() {
          webpages[j].syncTimeForOcr(videoTagsId[j], peerConnectionsId[j]);
          latch.countDown();
        }
      });
    }
    latch.await();
    service.shutdown();
  }

  public void serializeObject(Object object, String file) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(object);
    oos.close();
    fos.close();
  }

  public Table<Integer, Integer, String> processOcrAndStats(
      final Map<String, Map<String, Object>> presenter,
      final Map<String, Map<String, Object>> viewer) throws InterruptedException, IOException {

    log.debug("Processing OCR and stats");
    log.trace("Presenter {} : {}", presenter.size(), presenter.keySet());
    log.trace("Viewer {} : {}", viewer.size(), viewer.keySet());

    final Table<Integer, Integer, String> resultTable = HashBasedTable.create();
    final int numRows = presenter.size();
    final int threadPoolSize = Runtime.getRuntime().availableProcessors();
    final ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
    final CountDownLatch latch = new CountDownLatch(numRows);
    final Iterator<String> iteratorPresenter = presenter.keySet().iterator();

    // Process OCR (in parallel)
    for (int i = 0; i < numRows; i++) {
      final int j = i;
      final String key = iteratorPresenter.next();

      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            String matchKey = containSimilarDate(key, viewer.keySet());
            if (matchKey != null) {
              String presenterBase64 = presenter.get(key).get(LATENCY_KEY).toString();
              String viewerBase64 = viewer.get(matchKey).get(LATENCY_KEY).toString();
              String presenterDateStr = ocr(presenterBase64);
              String viewerDateStr = ocr(viewerBase64);
              String latency = String.valueOf(
                  processOcr(presenterDateStr, viewerDateStr, presenterBase64, viewerBase64));
              synchronized (resultTable) {
                if (!resultTable.row(0).containsValue(LATENCY_KEY)) {
                  resultTable.put(0, 0, LATENCY_KEY);
                }
                resultTable.put(j + 1, 0, latency);
              }
            }
          } finally {
            latch.countDown();
          }
        }
      });
    }
    latch.await();
    executor.shutdown();

    // Process statistics
    processStats(presenter, resultTable);
    processStats(viewer, resultTable);

    log.debug("OCR + Stats results: {}", resultTable);

    return resultTable;
  }

  public synchronized long processOcr(String presenterDateStr, String viewerDateStr,
      String presenterBase64, String viewerBase64) {
    long latency = -1;
    try {
      Date presenterDate = DATE_FORMAT.parse(presenterDateStr);
      Date viewerDate = DATE_FORMAT.parse(viewerDateStr);
      latency = presenterDate.getTime() - viewerDate.getTime();
    } catch (Exception e) {
      log.warn(
          "Unparseable date(s) (presenter: '{}' - viewer: '{}')" + "\nBase64 presenter: {}"
              + "\nBase64 viewer: {}",
          presenterDateStr, viewerDateStr, presenterBase64, viewerBase64, e);
    }
    log.debug("--> Latency {} ms (presenter: '{}' - viewer: '{}')", latency, presenterDateStr,
        viewerDateStr);

    // Debug trace for latencies over 1 second (or lower than -1)
    if (latency > 1000 || latency < -1) {
      log.trace(
          ">>> Bad latency measurement: {} ms (presenter: '{}' - viewer: '{}')"
              + "\nBase64 presenter: {}" + "\nBase64 viewer: {}",
          latency, presenterDateStr, viewerDateStr, presenterBase64, viewerBase64);
    }
    return latency;
  }

  public void processStats(Map<String, Map<String, Object>> stats,
      Table<Integer, Integer, String> resultTable) {
    Iterator<String> iterator = stats.keySet().iterator();
    for (int i = 0; i < stats.size(); i++) {
      String mapKey = iterator.next();
      Map<String, Object> entryStat = stats.get(mapKey);
      for (String key : entryStat.keySet()) {
        if (key.equalsIgnoreCase(LATENCY_KEY)) {
          continue;
        }
        if (!resultTable.row(0).containsValue(key)) {
          int columnCount = resultTable.columnKeySet().size();
          resultTable.put(0, columnCount, key);
          resultTable.put(1 + i, columnCount, entryStat.get(key).toString());
          log.trace("Inserting new header for stat: {} on column {}", key, columnCount);
          log.trace("Inserting first value for stat: {} on row {} column {}", entryStat.get(key),
              (1 + i), columnCount);
        } else {
          int columnIndex = getKeyOfValue(resultTable.row(0), key);
          resultTable.put(1 + i, columnIndex, entryStat.get(key).toString());
          log.trace("Inserting value for stat: {} on row {} column {}", entryStat.get(key), (1 + i),
              columnIndex);
        }
      }
    }
  }

  public void writeCSV(String outputFile, Table<Integer, Integer, String> resultTable)
      throws IOException {
    FileWriter writer = new FileWriter(outputFile);
    for (Integer row : resultTable.rowKeySet()) {
      boolean first = true;
      for (Integer column : resultTable.columnKeySet()) {
        if (!first) {
          writer.append(',');
        }
        String value = resultTable.get(row, column);
        if (value != null) {
          writer.append(value);
        }
        first = false;
      }
      writer.append('\n');
    }
    writer.flush();
    writer.close();
  }

  public void writeCSV(String outputFile, Multimap<String, Object> multimap, boolean orderKeys)
      throws IOException {
    FileWriter writer = new FileWriter(outputFile);

    // Header
    boolean first = true;
    Set<String> keySet = orderKeys ? new TreeSet<String>(multimap.keySet()) : multimap.keySet();
    for (String key : keySet) {
      if (!first) {
        writer.append(',');
      }
      writer.append(key);
      first = false;
    }
    writer.append('\n');

    // Values
    int i = 0;
    boolean moreValues;
    do {
      moreValues = false;
      first = true;
      for (String key : keySet) {
        Object[] array = multimap.get(key).toArray();
        moreValues = i < array.length;
        if (moreValues) {
          if (!first) {
            writer.append(',');
          }
          writer.append(array[i].toString());
        }
        first = false;
      }
      i++;
      if (moreValues) {
        writer.append('\n');
      }
    } while (moreValues);

    writer.flush();
    writer.close();
  }

  public Integer getKeyOfValue(Map<Integer, String> map, String value) {
    Integer key = null;
    for (Integer i : map.keySet()) {
      if (map.get(i).equalsIgnoreCase(value)) {
        key = i;
        break;
      }
    }
    return key;
  }

  public String containSimilarDate(String key, Set<String> keySet) {
    long minDiff = 0;
    for (String k : keySet) {
      long diff = Math.abs(Long.parseLong(key) - Long.parseLong(k));
      if (diff < OCR_TIME_THRESHOLD_MS) {
        return k;
      }
      if (minDiff == 0) {
        minDiff = diff;
      } else if (diff < minDiff) {
        minDiff = diff;
      }
    }
    log.warn("Not matching key for {} [min difference {}]", key, minDiff);
    return null;
  }

  public String ocr(String imgBase64) {
    // Base64 to BufferedImage
    BufferedImage imgBuff = null;
    try {
      imgBuff = ImageIO.read(new ByteArrayInputStream(
          Base64.decodeBase64(imgBase64.substring(imgBase64.lastIndexOf(",") + 1))));
    } catch (IOException e) {
      log.warn("IOException converting image to buffer", e);
    }
    return ocr(imgBuff);
  }

  public String ocr(BufferedImage imgBuff) {
    String parsedOut = null;

    try {
      // Color image to pure black and white
      for (int x = 0; x < imgBuff.getWidth(); x++) {
        for (int y = 0; y < imgBuff.getHeight(); y++) {
          Color color = new Color(imgBuff.getRGB(x, y));
          int red = color.getRed();
          int green = color.getBlue();
          int blue = color.getGreen();
          if (red + green + blue > OCR_COLOR_THRESHOLD) {
            red = green = blue = 0; // Black
          } else {
            red = green = blue = 255; // White
          }
          Color col = new Color(red, green, blue);
          imgBuff.setRGB(x, y, col.getRGB());
        }
      }

      // OCR recognition
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(imgBuff, "png", baos);
      byte[] imageBytes = baos.toByteArray();

      TessBaseAPI api = new TessBaseAPI();
      api.Init(null, "eng");
      ByteBuffer imgBB = ByteBuffer.wrap(imageBytes);

      PIX image = pixReadMem(imgBB, imageBytes.length);
      api.SetImage(image);

      // Get OCR result
      BytePointer outText = api.GetUTF8Text();

      // Destroy used object and release memory
      api.End();
      api.close();
      outText.deallocate();
      pixDestroy(image);

      // OCR corrections
      parsedOut = outText.getString().replaceAll("l", "1").replaceAll("Z", "2").replaceAll("O", "0")
          .replaceAll("B", "8").replaceAll("G", "6").replaceAll("S", "8").replaceAll("'", "")
          .replaceAll("‘", "").replaceAll("\\.", ":").replaceAll("E", "8").replaceAll("o", "0")
          .replaceAll("ﬂ", "0").replaceAll("ﬁ", "6").replaceAll("§", "5").replaceAll("I", "1")
          .replaceAll("T", "7").replaceAll("’", "").replaceAll("U", "0").replaceAll("D", "0");
      if (parsedOut.length() > 7) {
        parsedOut = parsedOut.substring(0, 7) + ":" + parsedOut.substring(8, parsedOut.length());
      }
      parsedOut = parsedOut.replaceAll("::", ":");

      // Remove last part (number of frames)
      int iSpace = parsedOut.lastIndexOf(" ");
      if (iSpace != -1) {
        parsedOut = parsedOut.substring(0, iSpace);
      }
    } catch (IOException e) {
      log.warn("IOException in OCR", e);
    }
    return parsedOut;
  }

  public File convertToRaw(File inputFile, File tmpFolder, double fps) {
    File y4m = new File(tmpFolder.toString() + File.separator + inputFile.getName() + Y4M);
    String[] ffmpegCommand = { "ffmpeg", "-i", inputFile.toString(), "-f", "yuv4mpegpipe", "-r",
        parseFps(fps), y4m.toString() };
    log.debug("Running command to convert to raw: {}", Arrays.toString(ffmpegCommand));
    Shell.runAndWait(ffmpegCommand);
    return y4m;
  }

  public Multimap<String, Object> getVideoQuality(File inputFile1, File inputFile2,
      String videoAlgorithm, double fps, int blocksize) throws IOException {
    String qpsnrCommand = "qpsnr -a " + videoAlgorithm + " -o blocksize=" + blocksize + ":fpa="
        + parseFps(fps) + " -r " + inputFile1.getAbsolutePath() + " "
        + inputFile2.getAbsolutePath();
    log.debug("Running qpsnr to calcule video quality ({}): {}", videoAlgorithm, qpsnrCommand);

    String outputShell[] = Shell.runAndWait("sh", "-c", qpsnrCommand).split("\\r?\\n");
    Multimap<String, Object> outputMap = ArrayListMultimap.create();
    boolean insertValues = false;
    for (String s : outputShell) {
      if (s.startsWith("Sample,")) {
        insertValues = true;
        continue;
      }
      if (insertValues) {
        outputMap.put(videoAlgorithm, s.split(",")[1]);
      }
    }
    return outputMap;
  }

  public String parseFps(double fps) {
    DecimalFormat df = new DecimalFormat("0");
    return df.format(fps);
  }

  public File cutVideo(File inputFile, File tmpFolder, int cutFrame, double fps) {
    double cutTime = cutFrame / fps;
    DecimalFormat df = new DecimalFormat("0.00");
    File cutVideoFile = new File(
        tmpFolder.toString() + File.separator + "cut-" + inputFile.getName());
    String[] command = { "ffmpeg", "-i", inputFile.getAbsolutePath(), "-ss", df.format(cutTime),
        cutVideoFile.getAbsolutePath() };
    log.debug("Running command to cut video: {}", Arrays.toString(command));
    Shell.runAndWait(command);
    return cutVideoFile;
  }

  public File cutAndTranscodeVideo(File inputFile, File tmpFolder, int cutFrame, double fps) {
    double cutTime = cutFrame / fps;
    DecimalFormat df = new DecimalFormat("0.00");
    File cutVideoFile = new File(
        tmpFolder.toString() + File.separator + "cut-" + inputFile.getName());
    String[] command = { "ffmpeg", "-i", inputFile.getAbsolutePath(), "-ss", df.format(cutTime),
        "-acodec", "copy", "-codec:v", "libvpx", cutVideoFile.getAbsolutePath() };
    log.debug("Running command to cut video: {}", Arrays.toString(command));
    Shell.runAndWait(command);
    return cutVideoFile;
  }

  public File transcodeVideo(File inputFile, File tmpFolder, double fps) {
    File transVideoFile = new File(
        tmpFolder.toString() + File.separator + "trans-" + inputFile.getName());
    String[] command = { "ffmpeg", "-i", inputFile.getAbsolutePath(), "-acodec", "copy", "-codec:v",
        "libvpx", transVideoFile.getAbsolutePath() };
    log.debug("Running command to transcode video: {}", Arrays.toString(command));
    Shell.runAndWait(command);
    return transVideoFile;
  }

  public int getCutFrame(final File inputFile1, final File inputFile2, File tmpFolder)
      throws IOException {
    // Filters to distinguish frames from file1 to file2
    FilenameFilter fileNameFilter1 = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.contains(inputFile1.getName()) && name.endsWith(PNG);
      }
    };
    FilenameFilter fileNameFilter2 = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.contains(inputFile2.getName()) && name.endsWith(PNG);
      }
    };

    File[] ls1 = tmpFolder.listFiles(fileNameFilter1);
    File[] ls2 = tmpFolder.listFiles(fileNameFilter2);
    Arrays.sort(ls1);
    Arrays.sort(ls2);

    List<String> ocrList1 = new ArrayList<>();
    List<String> ocrList2 = new ArrayList<>();
    int i = 0;
    for (; i < Math.min(ls1.length, ls2.length); i++) {
      String ocr1 = this.ocr(ImageIO.read(ls1[i]));
      String ocr2 = this.ocr(ImageIO.read(ls2[i]));
      ocrList1.add(ocr1);
      ocrList2.add(ocr2);

      log.trace("---> Time comparsion to find cut frame: {} vs {}", ocr1, ocr2);
      if (ocrList2.contains(ocr1)) {
        log.debug("Found OCR match {} at position {}", ocr1, i);
        // TODO Hack here: if the first video should be cut (presenter), the result is negative.
        // Otherwise the result is positive (cut the second video, i.e. the viewer)
        i *= -1;
        break;
      } else if (ocrList1.contains(ocr2)) {
        log.debug("Found OCR match {} at position {}", ocr2, i);
        break;
      }
    }
    return i;
  }

  public void getFrames(final File inputFile, final File tmpFolder) {
    Thread t = new Thread() {
      @Override
      public void run() {
        String[] command = { "ffmpeg", "-i", inputFile.getAbsolutePath(),
            tmpFolder.toString() + File.separator + inputFile.getName() + "-%03d" + PNG };
        log.debug("Running command to get frames: {}", Arrays.toString(command));
        Shell.runAndWait(command);
      }
    };
    t.start();
    waitMilliSeconds(500);
    t.interrupt();
  }

  public Multimap<String, Object> getSsim(File inputFile1, File inputFile2) throws IOException {
    return getVideoQuality(inputFile1, inputFile2, SSIM_KEY, FPS, BLOCKSIZE);
  }

  public Multimap<String, Object> getPsnr(File inputFile1, File inputFile2) throws IOException {
    return getVideoQuality(inputFile1, inputFile2, PSNR_KEY, FPS, BLOCKSIZE);
  }

  public void waitForFilesInFolder(String folder, final String ext, int expectedFilesNumber) {
    File dir = new File(folder);
    File[] files = null;
    do {
      if (files != null) {
        waitMilliSeconds(500); // polling
      }
      files = dir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.toLowerCase().endsWith(ext);
        }
      });
      log.debug("Number of files with extension {} in {} = {} (expected {})", ext, folder,
          files.length, expectedFilesNumber);
    } while (files.length != expectedFilesNumber);
  }

  public void addColumnsToTable(Table<Integer, Integer, String> table,
      Multimap<String, Object> column, int columnKey) {
    for (String key : column.keySet()) {
      shiftTable(table, columnKey);
      table.put(0, columnKey, key);
      Collection<Object> content = column.get(key);
      Iterator<Object> iterator = content.iterator();
      log.debug("Adding columun {} ({} elements) to table in position {}", key, content.size(),
          columnKey);
      for (int i = 0; i < content.size(); i++) {
        table.put(i + 1, columnKey, iterator.next().toString());
      }
      columnKey++;
    }
  }

  private void shiftTable(Table<Integer, Integer, String> table, int columnKey) {
    for (int i = table.columnKeySet().size() - 1; i >= columnKey; i--) {
      Map<Integer, String> column = table.column(i);
      for (int j : column.keySet()) {
        table.put(j, i + 1, column.get(j));
      }
    }
    table.column(columnKey).clear();
  }

}
