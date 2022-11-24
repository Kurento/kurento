/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.TestConfiguration.KMS_STUN_IP_PROPERTY;
import static org.kurento.test.config.TestConfiguration.KMS_STUN_PORT_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_CONFIG_JSON_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_DISK_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_DISK_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_DISK_PROP_OLD;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_HTTP_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_HTTP_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_MONGO_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_MONGO_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_S3_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_S3_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_S3_PROP_OLD;
import static org.kurento.test.config.TestConfiguration.TEST_FILES_URL_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_SERVER_CREDENTIAL_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_SERVER_URL_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_SERVER_USERNAME_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NUMRETRIES_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NUM_NUMRETRIES_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PRINT_LOG_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PRINT_LOG_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_PROJECT_PATH_DEFAULT;
import static org.kurento.test.config.TestConfiguration.TEST_PROJECT_PATH_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_RECORD_DEFAULTPATH_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_RECORD_URL_PROP;
import static org.kurento.test.config.TestConfiguration.TEST_SEEK_REPETITIONS;
import static org.kurento.test.config.TestConfiguration.TEST_SEEK_REPETITIONS_DEFAULT;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import org.kurento.commons.ConfigFileManager;
import org.kurento.test.config.Retry;
import org.kurento.test.config.TestReport;
import org.kurento.test.config.TestScenario;
import org.kurento.test.lifecycle.FailedTest;
import org.kurento.test.lifecycle.FinishedTest;
import org.kurento.test.lifecycle.KurentoBlockJUnit4ClassRunnerWithParametersFactory;
import org.kurento.test.lifecycle.KurentoTestRunner;
import org.kurento.test.lifecycle.KurentoTestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for Kurento tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */
@RunWith(KurentoTestRunner.class)
@UseParametersRunnerFactory(KurentoBlockJUnit4ClassRunnerWithParametersFactory.class)
public class KurentoTest {

  @Rule
  public Retry retry = new Retry(numRetries);

  @Rule
  public KurentoTestWatcher watcher = new KurentoTestWatcher();

  @Rule
  public TestName name = new TestName();
  
  @Parameter
  public TestScenario testScenario;

  @Parameters
  public static Collection<Object[]> data() {
    return TestScenario.empty();
  }

  protected static int numRetries =
      getProperty(TEST_NUMRETRIES_PROPERTY, TEST_NUM_NUMRETRIES_DEFAULT);
  protected static String testDir = getProperty(TEST_PROJECT_PATH_PROP, TEST_PROJECT_PATH_DEFAULT);
  protected static boolean printLogs = getProperty(TEST_PRINT_LOG_PROP, TEST_PRINT_LOG_DEFAULT);

  public static Logger log = LoggerFactory.getLogger(KurentoTest.class);

  protected static String testIdentifier;
  protected static String testMethodName;
  protected static String testClassName;
  protected static List<File> logFiles;
  protected static boolean deleteLogsIfSuccess;

  public static final String SEPARATOR = "+" + StringUtils.repeat("-", 70);

  static {
    ConfigFileManager.loadConfigFile(TEST_CONFIG_JSON_DEFAULT);
  }

  public KurentoTest() {
    testClassName = this.getClass().getName();
    testIdentifier = this.getClass().getSimpleName() + " [" + new Date() + "]";
    retry.useReport(testIdentifier);
    deleteLogsIfSuccess = true;
  }

  @FailedTest
  public static void printKmsLogs() {
    if (printLogs) {
      if (logFiles != null) {
        for (File logFile : logFiles) {
          if (logFile != null && logFile.exists()) {
            System.err.println(SEPARATOR);
            System.err.println("Log file: " + logFile.getAbsolutePath());
            try {
              for (String line : FileUtils.readLines(logFile)) {
                System.err.println(line);
              }
            } catch (Throwable e) {
              log.warn("Error reading log file {}: {} {}", logFile, e.getClass(), e.getMessage());
            }
            System.err.println(SEPARATOR);
          }
        }
      }
    }
  }

  @FinishedTest
  public static void deleteLogs() {
    if (KurentoTestWatcher.isSuccees() && deleteLogsIfSuccess) {
      File folder = KurentoTest.getDefaultOutputFolder();
      final File[] files = folder.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
          return name.contains(KurentoTest.getSimpleTestName());
        }
      });

      if (files != null) {
        for (final File file : files) {
          try {
            if (file.isDirectory()) {
              FileUtils.deleteDirectory(file);
            } else {
              file.delete();
            }
            if (file.exists()) {
              log.error("Can't remove {}", file.getAbsolutePath());
            }
          } catch (Throwable e) {
            log.warn("Exception deleting file {}: {} {}", file, e.getClass(), e.getMessage());
          }
        }
      }
    }
  }

  
  @Before
  public void logStart() {
      log.info("##### Start test: " + this.getClass().getSimpleName() + " -> " + name.getMethodName());
  }

  @After
  public void logEnd() {
      log.info("##### Finish test: " + this.getClass().getSimpleName() + " -> " + name.getMethodName());
  }

  
  // @Before
  // public void setupKurentoTest() {
  // logMessage("| TEST STARTING: " + getTestClassName() + "."
  // + getTestMethodName());
  // }
  //
  // @After
  // public void teardownKurentoTest() {
  // logMessage("| TEST FINISHED: " + getTestClassName() + "."
  // + getTestMethodName());
  // }

  public TestReport getTestReport() {
    return retry.getTestReport();
  }

  public static File getDefaultOutputFolder() {
    File testResultsFolder = new File(testDir + File.separator + testClassName);

    if (!testResultsFolder.exists()) {
      testResultsFolder.mkdirs();
    }
    return testResultsFolder;
  }

  public static String getDefaultOutputFile(String suffix) {
    return getDefaultOutputFolder().getAbsolutePath() + File.separator + getSimpleTestName()
        + suffix;
  }

  public static String getDefaultOutputTestPath() {
    return getDefaultOutputFolder().getAbsolutePath() + File.separator + getSimpleTestName()
        + File.separator;
  }

  public static String getRecordDefaultPath() {
    return getProperty(TEST_RECORD_DEFAULTPATH_PROP);
  }

  public static String getRecordUrl(String suffix) {
    String recordUrl = getProperty(TEST_RECORD_URL_PROP);
    if (recordUrl == null) {
      return FILE + "://" + getDefaultOutputFile(suffix);
    }
    return recordUrl + File.separator + getSimpleTestName() + suffix;
  }

  public static String getPlayerUrl(String mediaName) {
    String playerUrl = getProperty(TEST_FILES_URL_PROP);
    if (playerUrl == null) {
      return HTTP + "://" + getTestFilesHttpPath() + mediaName;
    }
    return playerUrl + mediaName;
  }

  public static String getSimpleTestName() {
    String out = testMethodName;
    if (testMethodName != null && out.indexOf(":") != -1) {
      out = out.substring(0, out.indexOf(":")) + "]";
    }
    return out;
  }

  public static String getTestMethodName() {
    return testMethodName;
  }

  public static void setTestMethodName(String testMethodName) {
    KurentoTest.testMethodName = testMethodName;
  }

  public static String getTestClassName() {
    return testClassName;
  }

  public static void setTestClassName(String testClassName) {
    KurentoTest.testClassName = testClassName;
  }

  public static String getTestDir() {
    return testDir;
  }

  public static void setTestDir(String testDir) {
    KurentoTest.testDir = testDir;
  }

  public static String getTestIdentifier() {
    return testIdentifier;
  }

  public static void setTestIdentifier(String testIdentifier) {
    KurentoTest.testIdentifier = testIdentifier;
  }

  public static boolean isDeleteLogsIfSuccess() {
    return deleteLogsIfSuccess;
  }

  public static void setDeleteLogsIfSuccess(boolean deleteLogsIfSuccess) {
    KurentoTest.deleteLogsIfSuccess = deleteLogsIfSuccess;
  }

  public static void addLogFile(File logFile) {
    log.debug("Adding log file: {}", logFile);
    if (logFiles == null) {
      logFiles = new ArrayList<>();
    }
    logFiles.add(logFile);
  }

  public static List<File> getServerLogFiles() {
    int countFiles = logFiles != null ? logFiles.size() : 0;
    log.debug("Logs files {}", countFiles);
    return logFiles;
  }

  public static String getTestFilesDiskPath() {
    String testFilesDisk = getProperty(TEST_FILES_DISK_PROP);
    if (testFilesDisk == null) {
      testFilesDisk = getProperty(TEST_FILES_DISK_PROP_OLD, TEST_FILES_DISK_DEFAULT);
    }
    return testFilesDisk;
  }

  public static String getTestFilesS3Path() {
    String testFilesS3 = getProperty(TEST_FILES_S3_PROP);
    if (testFilesS3 == null) {
      testFilesS3 = getProperty(TEST_FILES_S3_PROP_OLD, TEST_FILES_S3_DEFAULT);
    }
    return testFilesS3;
  }

  public static String getTestFilesHttpPath() {
    return getProperty(TEST_FILES_HTTP_PROP, TEST_FILES_HTTP_DEFAULT);
  }

  public static String getTestFilesMongoPath() {
    return getProperty(TEST_FILES_MONGO_PROP, TEST_FILES_MONGO_DEFAULT);
  }

  public static void logMessage(String message) {
    log.debug(SEPARATOR);
    log.debug(message);
    log.debug(SEPARATOR);
  }

  public static int getTestSeekRepetitions() {
    return getProperty(TEST_SEEK_REPETITIONS, TEST_SEEK_REPETITIONS_DEFAULT);
  }

  public static String getTestStunServerUrl() {
    return "stun:" + getProperty(KMS_STUN_IP_PROPERTY) + ":" + getProperty(KMS_STUN_PORT_PROPERTY);
  }

  public static String getTestIceServerUrl() {
    return getProperty(TEST_ICE_SERVER_URL_PROPERTY);
  }

  public static String getTestIceServerUsername() {
    return getProperty(TEST_ICE_SERVER_USERNAME_PROPERTY);
  }

  public static String getTestIceServerCredential() {
    return getProperty(TEST_ICE_SERVER_CREDENTIAL_PROPERTY);
  }

}
