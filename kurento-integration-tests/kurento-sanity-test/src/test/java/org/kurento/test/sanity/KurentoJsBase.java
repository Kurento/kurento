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

package org.kurento.test.sanity;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.commons.testing.SanityTests;
import org.kurento.test.base.KurentoClientBrowserTest;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPage;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Base for kurento-js sanity tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SanityTests.class)
public class KurentoJsBase extends KurentoClientBrowserTest<WebPage> {

  protected static final Logger log = LoggerFactory.getLogger(KurentoJsBase.class);

  protected static final String DEFAULT_BOWER_RELEASE_URL = "//example.com/dev/master/latest/";

  protected String[] kurentoLibs =
      { "kurento-client", "kurento-client.min", "kurento-utils", "kurento-utils.min" };

  protected String kurentoUrl;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    TestScenario test = new TestScenario();
    test.addBrowser(BrowserConfig.BROWSER,
        new Browser.Builder().browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).build());

    return Arrays.asList(new Object[][] { { test } });
  }

  @Before
  public void setup() {
    try {
      final String outputFolder =
          new ClassPathResource("static").getFile().getAbsolutePath() + File.separator;

      Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
      cfg.setClassForTemplateLoading(KurentoJsBase.class, "/templates/");
      Template template = cfg.getTemplate("kurento-client.html.ftl");

      Map<String, Object> data = new HashMap<String, Object>();
      data.put("kurentoUrl", kurentoUrl);

      for (String lib : kurentoLibs) {
        Writer writer = new FileWriter(new File(outputFolder + lib + ".html"));
        data.put("kurentoLib", lib);

        if (lib.contains("utils")) {
          data.put("kurentoObject", "kurentoUtils");
        } else {
          data.put("kurentoObject", "kurentoClient");
        }

        template.process(data, writer);
        writer.flush();
        writer.close();
      }
    } catch (Exception e) {
      Assert.fail("Exception creating templates: " + e.getMessage());
    }

  }

  public void doTest() {
    String defaultUrl = getPage().getBrowser().getWebDriver().getCurrentUrl();

    for (String lib : kurentoLibs) {
      String urlTest = defaultUrl + lib + ".html";
      getPage().getBrowser().getWebDriver().get(urlTest);

      log.debug("Launching kurento-js sanity test against {}", urlTest);

      String status =
          getPage().getBrowser().getWebDriver().findElement(By.id("status")).getAttribute("value");

      Assert.assertTrue("Sanity test for " + lib + " failed (" + status + ")", status.equals("Ok"));
    }
  }

}
