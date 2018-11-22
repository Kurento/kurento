:orphan:

..
   Hidden section. When some contents are added:
   - Remove the :orphan: tag
   - Remove this comment
   - Un-comment the section's name in the index file

=======
Testing
=======

Software testing is a broad term within software engineering encompassing a wide spectrum of different concepts. Depending on the size of the System Under Test (SUT) and the scenario in which it is exercised, testing can be carried out at different levels. There is no universal classification for all the different testing levels. Nevertheless, the following levels are broadly accepted:

- Unit: individual program units are tested. Unit tests typically focus on the functionality of individual objects or methods.
- Integration: units are combined to create composite components. Integration tests focus on the interaction of different units.
- System: all of the components are integrated and the system is tested as a whole.

There is a special type of system tests called **end-to-end** (E2E). In E2E tests, the final user is typically impersonated, i.e., simulated using automation techniques. The main benefit of E2E tests is the simulation of real user scenarios in an automated fashion.

E2E Tests
=========

This section introduces the different types of E2E implemented with KTF for Kurento, namely **functional**, **stability**, **tutorials**, and **API**.

Functional
----------

To do.

Stability
---------

To do.

Tutorials
---------

To do.

API
---

To do.

Running tests
=============

This section explains the KTF API.

In local environemnt
--------------------

This section explains how to use the KTF API for running Kurento tests in a local environment.

In Jenkins
----------

This section explains how to use the KTF API for running Kurento tests in a Jenkins Continuous Integration (CI) server.

Kurento Testing Framework explained
===================================

In order to assess properly Kurento from a final user perspective, a rich suite of E2E tests has been designed and implemented. To that aim, the **Kurento Testing Framework** (KTF) has been created. KTF is a part of the Kurento project aimed to carry out end-to-end (E2E) tests for Kurento. KTF has been implemented on the top of two well-known open-source testing frameworks: `JUnit <https://junit.org/>`_ and `Selenium <https://www.seleniumhq.org/>`_.

KTF provides high level capabilities to perform advanced automated testing for Kurento-based applications. KTF has been implemented in Java, and as usual it is hosted on GitHub, in the project `kurento-test <https://github.com/Kurento/kurento-java/tree/master/kurento-integration-tests/kurento-test>`_. KTF has been designed on the top of **JUnit 4**, providing a rich hierarchy of classes which are going to act as parent for JUnit 4 tests cases. This hierarchy is the following:

.. figure:: ../images/ktf-class-diagram.png
   :align:   center
   :alt:     Kurento Testing Framework class hierarchy

   *Kurento Testing Framework class hierarchy*

The most important classes of this diagram are the following:

- `KurentoTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/KurentoTest.java>`_: Top class of the KTF. It provides different features out-of-the-box for tests extending this class, namely:

   - Improved test lifecycle: KTF enhances the lyfecycle of JUnit 4 test cases, watching the result of tests (passed, failed). Moreover, KTF provides extra annotations to be used in different parts of the test lifecycle, such as `FailedTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/FailedTest.java>`_, `FinishedTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/FinishedTest.java>`_, `FinishedTestClass <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/FinishedTestClass.java>`_, `StartedTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/StartedTest.java>`_, `StartedTestClass <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/StartedTestClass.java>`_, or `SucceededTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/SucceededTest.java>`_.

   - Reporting: An HTML report summarizing the results of a test suite executed with KTF is automatically created for Kurento tests. This report is called ``report.html`` and it is located by default on the ``target`` folder when tests are executed with Maven.

   - Retries mechanism: In order to detect flaky tests, a retries mechanism is present in KTF. This mechanism allows to repeat a failed test a configurable number of times.

- `KurentoClientTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/KurentoClientTest.java>`_: It provides an instance of **Kurento Media Server** (KMS) together with a instance of a **Kurento Java Client** to control KMS. There are two options to run this KMS (see KTF API section for configuration details):

   - Local KMS. To use this option, it is a pre-requisite to have KMS installed in the machine running this type of tests.

   - KMS in a **Docker** container. To use this option, it is a pre-requisite to have `Docker <https://www.docker.com/>`_ installed in the machine running this type of tests.

- `BrowserTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/BrowserTest.java>`_: This class provides wrappers of `Selenium WebDriver <https://www.seleniumhq.org/projects/webdriver/>`_ instances aimed to control a group of web browsers for tests. By default, KTF allows to use **Chrome** or **Firefox** as browsers. The scope of these browsers can be configured to use:

   - Local browser, i.e. installed in the local machine.

   - Remote browser, i.e. installed in the remote machines (using Selenium Grid).

   - Docker browsers, i.e. executed in `Docker <https://www.docker.com/>`_ containers.

   - Saucelabs browsers. `Saucelabs <https://saucelabs.com/>`_ is a cloud solution for web testing. It provides a big number of browsers to be used in Selenium tests. KTF provides seamless integration with Saucelabs.

   Test scenario can be configured in ``BrowserTest`` tests in two different ways:

   - Programmatically using Java. Test scenario uses JUnit 4's parameterized feature. The Java class `TestScenario <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/config/TestScenario.java>`_ is used by KTF to configure the scenario, for example as follows:

   .. code-block:: java

      @Parameters(name = "{index}: {0}")
      public static Collection<Object[]> data() {
         TestScenario test = new TestScenario();
         test.addBrowser(BrowserConfig.BROWSER, new Browser.Builder().browserType(BrowserType.CHROME)
             .scope(BrowserScope.LOCAL).webPageType(webPageType).build());

         return Arrays.asList(new Object[][] { { test } });
      }

   - Using a JSON file. KTF allows to setup tests scenarios based on a custom customizable JSON notation. In these JSON files, several test executions can be setup. For each execution, the browser scope can be chosen. For example, the following example shows a test scenario in which two executions are defined. First execution defines two local browsers (identified as peer1 and peer2), Chrome and Firefox respectively. The second execution defines also two browsers, but this time browsers are located in the cloud infrastructure provided by Saucelabs.

   .. code-block:: json

      {
         "executions":[
            {
               "peer1":{
                  "scope":"local",
                  "browser":"chrome"
               },
               "peer2":{
                  "scope":"local",
                  "browser":"firefox"
               }
            },
            {
               "peer1":{
                  "scope":"saucelabs",
                  "browser":"explorer",
                  "version":"11"
               },
               "peer2":{
                  "scope":"saucelabs",
                  "browser":"safari",
                  "version":"36"
               }
            }
         ]
      }

- `KurentoClientBrowserTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/KurentoClientBrowserTest.java>`_: This class can be seen as a mixed of the previous ones, since it provides the capability to use KMS (local or *dockerized*) together with a group of browser test using a *test scenario*. Moreover, it provides a web server started with each test for testing purposed, with a custom `web page <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/resources/static/webrtc.html>`_ available to test **WebRTC** in Kurento in a easy manner. As can be seen in the diagram before, this class is the parent of a rich variety of different classes. In short, these classes are used to distinguish among different types of tests. See next section for more information.
