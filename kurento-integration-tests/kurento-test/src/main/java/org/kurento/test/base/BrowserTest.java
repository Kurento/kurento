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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Base for Kurento tests that use browsers.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public abstract class BrowserTest<W extends WebPage> extends KurentoTest {

  public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);
  public static final int OCR_COLOR_THRESHOLD = 180;
  public static final int OCR_TIME_THRESHOLD_MS = 100;

  private Map<String, W> pages = new ConcurrentHashMap<>();

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

  private void initBrowser(String browserKey, Browser browser) {
    browser.setId(browserKey);
    browser.setName(getTestMethodName());
    browser.init();
    browser.injectKurentoTestJs();
  }

  @After
  public void teardownBrowserTest() {
    if (testScenario != null) {
      for (Browser browser : testScenario.getBrowserMap().values()) {
        try {
          browser.close();
        } catch (Exception e) {
          log.warn("Exception closing browser {}", browser.getId(), e);
        }
      }
    }
  }

  public TestScenario getTestScenario() {
    return testScenario;
  }

  public void addBrowser(String browserKey, Browser browser) {
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
        Assert.fail("URL " + url + " not reachable. Response code=" + responseCode);
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

  public String ocr(String imgBase64) {
    String parsedOut = null;

    try {
      // Base64 to BufferedImage
      BufferedImage imgBuff = ImageIO.read(new ByteArrayInputStream(
          Base64.decodeBase64(imgBase64.substring(imgBase64.lastIndexOf(",") + 1))));

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
          .replaceAll("B", "8").replaceAll("S", "8").replaceAll("'", "");

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

  public String containSimilarDate(String key, Set<String> keySet) {
    for (String k : keySet) {
      long diff = Math.abs(Long.parseLong(key) - Long.parseLong(k));
      if (diff < OCR_TIME_THRESHOLD_MS) {
        return k;
      }
    }
    return null;
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

  public void processOcrDataToCsv(String outputFile, final Map<String, String> presenterOcr,
      final Map<String, String> viewerOcr, final List<Map<String, String>> presenterStats,
      final List<Map<String, String>> viewerStats) throws InterruptedException, IOException {

    log.info("Processing OCR and stats data to CSV ({})", outputFile);
    log.trace("Presenter OCR {} : {}", presenterOcr.size(), presenterOcr);
    log.trace("Viewer OCR {} : {}", viewerOcr.size(), viewerOcr);
    log.trace("Presenter Stats {} : {}", presenterStats.size(), presenterStats);
    log.trace("Viewer Stats {} : {}", viewerStats.size(), viewerStats);

    final Table<Integer, Integer, String> resultTable = HashBasedTable.create();
    final String latencyKey = "latencyMs";
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss:S");
    final int numRows = presenterOcr.size();
    final ExecutorService executor = Executors.newFixedThreadPool(numRows);
    final CountDownLatch latch = new CountDownLatch(numRows);
    final Iterator<String> iterator = presenterOcr.keySet().iterator();

    // Process OCR
    for (int i = 0; i < numRows; i++) {
      final int j = i;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            String key = iterator.next();
            String matchKey = containSimilarDate(key, viewerOcr.keySet());
            if (matchKey != null) {
              String presenterDateStr = ocr(presenterOcr.get(key));
              String viewerDateStr = ocr(viewerOcr.get(matchKey));
              long latency = -1;
              try {
                Date presenterDate = simpleDateFormat.parse(presenterDateStr);
                Date viewerDate = simpleDateFormat.parse(viewerDateStr);
                latency = presenterDate.getTime() - viewerDate.getTime();
              } catch (ParseException e) {
                log.warn(
                    "Unparseable date(s) (presenter: '{}' - viewer: '{}')"
                        + "\nBase64 presenter: {}" + "\nBase64 viewer: {}",
                    presenterDateStr, viewerDateStr, presenterOcr.get(key),
                    viewerOcr.get(matchKey));
              }
              log.debug("-----> [{}] Latency {} ms (presenter: '{}' - viewer: '{}')", j, latency,
                  presenterDateStr, viewerDateStr);

              // Debug trace for latencies over 1 second (or lower than -1)
              if (latency > 1000 || latency < -1) {
                log.warn(
                    "Bad latency measurement: {} ms (presenter: '{}' - viewer: '{}')"
                        + "\nBase64 presenter: {}" + "\nBase64 viewer: {}",
                    latency, presenterDateStr, viewerDateStr, presenterOcr.get(key),
                    viewerOcr.get(matchKey));
              }

              if (!resultTable.row(0).containsValue(latencyKey)) {
                resultTable.put(0, 0, latencyKey);
              }
              resultTable.put(j + 1, 0, String.valueOf(latency));
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
    processStats(presenterStats, resultTable);
    processStats(viewerStats, resultTable);

    log.info("OCR + Stats results: {}", resultTable);

    // Write CSV
    writeCSV(outputFile, resultTable);
  }

  public void processStats(List<Map<String, String>> stats,
      Table<Integer, Integer, String> resultTable) {
    for (int i = 0; i < stats.size(); i++) {
      Map<String, String> entryStat = stats.get(i);
      for (String key : entryStat.keySet()) {
        if (!resultTable.row(0).containsValue(key)) {
          int columnCount = resultTable.columnKeySet().size();
          resultTable.put(0, columnCount, key);
          resultTable.put(1 + i, columnCount, entryStat.get(key));
        } else {
          int columnIndex = getKeyOfValue(resultTable.row(0), key);
          resultTable.put(1 + i, columnIndex, entryStat.get(key));
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

}
