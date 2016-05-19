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

package org.kurento.test.base;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_URL_TIMEOUT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_URL_TIMEOUT_PROPERTY;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.TestScenario;
import org.kurento.test.internal.AbortableCountDownLatch;

import com.venky.ocr.TextRecognizer;

/**
 * Base for Kurento tests that use browsers.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public abstract class BrowserTest<W extends WebPage> extends KurentoTest {

  public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);

  public static final int OCR_COLOR_THRESHOLD = 255;

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

  public String ocr(String imgBase64) throws IOException {
    // Base64 to BufferedImage
    BufferedImage imgBuff = ImageIO.read(new ByteArrayInputStream(
        Base64.getDecoder().decode(imgBase64.substring(imgBase64.lastIndexOf(",") + 1))));

    // Color image to pure black and white
    for (int x = 0; x < imgBuff.getWidth(); x++) {
      for (int y = 0; y < imgBuff.getHeight(); y++) {
        Color color = new Color(imgBuff.getRGB(x, y));
        int red = color.getRed();
        int green = color.getBlue();
        int blue = color.getGreen();
        if (red + green + blue > OCR_COLOR_THRESHOLD) {
          red = green = blue = 0;
        } else {
          red = green = blue = OCR_COLOR_THRESHOLD;
        }
        Color col = new Color(red, green, blue);
        imgBuff.setRGB(x, y, col.getRGB());
      }
    }

    // OCR recognition
    TextRecognizer monospace = new TextRecognizer();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ImageIO.write(imgBuff, "png", os);
    InputStream is = new ByteArrayInputStream(os.toByteArray());
    StringBuffer out = monospace.recognize(is);

    return out.toString().replaceAll("O", "0");
  }

  public String containSimilarDate(String key, Set<String> keySet) throws ParseException {
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

  public void processOcrDataToCsv(String outputFile, Map<String, String> presenterOcr,
      Map<String, String> viewerOcr, List<Map<String, String>> presenterStats,
      List<Map<String, String>> viewerStats) throws ParseException, IOException {

    Map<String, List<String>> result = new TreeMap<>();
    final String latencyKey = "latencyMs";

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H:mm:ss:S");

    Iterator<String> iterator = presenterOcr.keySet().iterator();

    int empty = 0;
    for (int i = 0; i < presenterOcr.size(); i++) {
      String key = iterator.next();
      String matchKey = containSimilarDate(key, viewerOcr.keySet());
      if (matchKey != null) {
        Date presenterDater = simpleDateFormat.parse(ocr(presenterOcr.get(key)));
        Date viewerDater = simpleDateFormat.parse(ocr(viewerOcr.get(matchKey)));
        long latency = presenterDater.getTime() - viewerDater.getTime();
        log.trace("[{}] Latency {}", i, latency);

        // Latency
        List<String> latencyList = null;
        if (result.containsKey(latencyKey)) {
          latencyList = result.get(latencyKey);
        } else {
          latencyList = new ArrayList<>();
          result.put(latencyKey, latencyList);
        }
        latencyList.add(String.valueOf(latency));

        // Stats
        Map<String, String> allStats = new HashMap<>();
        if (i < presenterStats.size()) {
          allStats.putAll(presenterStats.get(i));
        }
        if (i < viewerStats.size()) {
          allStats.putAll(viewerStats.get(i));
        }

        // When stats are empty it means no value should be stored in the final result table.
        // Therefore an empty cell should appear in this data structure. To count this situation,
        // the variable empty counts the occurrence of this issue
        if (allStats.isEmpty()) {
          empty++;
        }

        for (String keyStat : allStats.keySet()) {
          List<String> statList = null;
          if (result.containsKey(keyStat)) {
            statList = result.get(keyStat);
          } else {
            statList = new ArrayList<>();
            for (int z = 0; z < empty; z++) {
              statList.add("");
            }
            result.put(keyStat, statList);
          }
          statList.add(allStats.get(keyStat));
        }
      }
    }

    log.trace("Final stats {} {}", result, result.size());

    // Write CSV
    FileWriter writer = new FileWriter(outputFile);
    boolean first = true;
    for (String key : result.keySet()) {
      if (!first) {
        writer.append(',');
      }
      writer.append(key);
      first = false;
    }
    writer.append('\n');

    int i = 0;
    while (true) {
      try {
        first = true;
        for (List<String> value : result.values()) {
          if (!first) {
            writer.append(',');
          }
          writer.append(value.get(i));
          first = false;
        }
        writer.append('\n');
        i++;
      } catch (Exception e) {
        e.printStackTrace();
        break;
      }
    }
    writer.flush();
    writer.close();

    log.trace("Presenter OCR {} : {}", presenterOcr.size(), presenterOcr.keySet());
    log.trace("Viewer OCR {} : {}", viewerOcr.size(), viewerOcr.keySet());
    log.trace("Presenter Stats {} : {}", presenterStats.size(), presenterStats);
    log.trace("Viewer Stats {} : {}", viewerStats.size(), viewerStats);
  }

}
