
package org.kurento.test;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.base.KurentoClientBrowserTest;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;

public class BrowserLogTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void test() throws Exception {
    getPage().browser.executeScript("console.log('This is a log message');");
    getPage().browser.executeScript("console.info('This is a info message');");
    getPage().browser.executeScript("console.error('This is an error message');");
    getPage().browser.executeScript("console.warn('This is a warning');");

    Assert.assertTrue(false);
  }
}
