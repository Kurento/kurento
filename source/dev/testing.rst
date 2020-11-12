=======
Testing
=======

Software testing is a broad term within software engineering encompassing a wide spectrum of different concepts. Depending on the size of the System Under Test (SUT) and the scenario in which it is exercised, testing can be carried out at different levels. There is no universal classification for all the different testing levels. Nevertheless, the following levels are broadly accepted:

- Unit: individual program units are tested. Unit tests typically focus on the functionality of individual objects or methods.

- Integration: units are combined to create composite components. Integration tests focus on the interaction of different units.

- System: all of the components are integrated and the system is tested as a whole.

There is a special type of system tests called **end-to-end** (E2E). In E2E tests, the final user is typically impersonated, i.e., simulated using automation techniques. The main benefit of E2E tests is the simulation of real user scenarios in an automated fashion. As described in the rest of this document, a rich variety of E2E has been implemented to assess Kurento.

E2E Tests
=========

This section introduces the different types of E2E implemented to assess different parts of Kurento, namely **functional**, **stability**, **tutorials**, and **API**.

Functional
----------

Functional tests are aimed to evaluate a given capability provided by Kurento. These tests have created in Java. You can find the source code in the repository `kurento-test <https://github.com/Kurento/kurento-java/tree/master/kurento-integration-tests/kurento-test>`_ within `kurento-java <https://github.com/Kurento/kurento-java/>`_.  In order to run functional tests, Maven should be used as follows:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento-java.git
   cd kurento-java
   mvn \
       --projects kurento-integration-tests/kurento-test --also-make \
       -Pintegration \
       -Dgroups=org.kurento.commons.testing.SystemFunctionalTests \
       clean verify


By default, these tests required a local Kurento Media Server installed in the machine running the tests. In addition, Chrome and Firefox browsers are also required. For further information about running these tests, please read next section.

The main types of functional tests for Kurento are the following:

- WebRTC. Real-time media in the web is one of the core Kurento capabilities, and therefore, a rich test suite to assess the use of WebRTC in Kurento has been implemented. Moreover, two special WebRTC features are also tested:

   - Datachannels. A WebRTC data channel allows to send custom data over an active connection to a peer. Tests using Chrome and Firefox has been implemented to check WebRTC datachannels.
   - ICE. In order to create media communication between peers avoiding :term:`NAT Traversal` problems, ICE (Interactive Connectivity Establishment) negotiation is used in WebRTC. Kurento ICE tests check this connectivity using different network setups (NATs, reflexive, bridge).

- Recorder. Another important capability provided by Kurento is the media archiving. Recorder tests use *RecorderEndpoint* media element by ensuring that the recorded media is as expected.

- Player. KMS's *PlayerEndpoint* allows to inject media from seekable or non-seekable sources to a media pipeline. A suite of tests have been implemented to assess this feature.

- Composite/Dispatcher. KMS allows to mix media using different media elements (*Composite* and *Dispatcher*). These tests are aimed to asses the result of this media mixing.

Stability
---------

Stability tests verifies Kurento capabilities in different scenarios:

- Running media pipelines in large amount of time.

- Using a lot of resources (CPU, memory) of a KMS instance.

Stability tests have been also created using Java, and they are contained in the project `kurento-test <https://github.com/Kurento/kurento-java/tree/master/kurento-integration-tests/kurento-test>`_. Again, we use Maven to execute stability tests against a local KMS and using also local browsers (Chrome, Firefox):

.. code-block:: shell

   git clone https://github.com/Kurento/kurento-java.git
   cd kurento-java
   mvn \
       --projects kurento-integration-tests/kurento-test --also-make \
       -Pintegration \
       -Dgroups=org.kurento.commons.testing.SystemStabilityTests \
       clean verify

Tutorials
---------

The documentation of Kurento includes a number of tutorials `tutorials <https://doc-kurento.readthedocs.io/en/latest/user/tutorials.html>`_ which allows to understand Kurento capabilities using ready to be used simple applications. Kurento tutorials have been developed for three technologies: Java, JavaScript, and Node.js. Moreover, for some of the Java tutorials, different E2E tests have been created. These tests are available in the project `kurento-tutorial-test <https://github.com/Kurento/kurento-tutorial-test/>`_. In order to run these tests, Maven should be used:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento-tutorial-test
   cd kurento-tutorial-test
   mvn clean verify

API
---

The `Kurento API <https://doc-kurento.readthedocs.io/en/latest/features/kurento_api.html>`_ is available in two languages: Java and JavaScript. For both of them, a test suite has been created to verify the correctness of the Kurento API against a running instance of KMS. In you want to run API tests for Java, as usual for Kurento tests, Maven is required, as follows:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento-java.git
   cd kurento-java
   mvn \
       --projects kurento-integration-tests/kurento-client-test --also-make \
       -Pintegration \
       -Dgroups=org.kurento.commons.testing.KurentoClientTests \
       clean verify

In order to run JavaScript API tests against a running instance of local KMS, the command to be used is the following:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento-client-js
   cd kurento-client-js
   npm install
   rm -f node_modules/kurento-client && ln -s .. node_modules/kurento-client
   npm test

Running Java tests
==================

Functional, stability, and Java API tests for Kurento have been created using a custom Java library called **Kurento Testing Framework** (KTF). For more details about this framework, please take a look to the next section. If you are interested only in running a group of functional or stability E2E tests in order to assess Kurento, please keep reading this section.

Maven is the the way which E2E Kurento are executed. Therefore, in order to run E2E tests, first we need in have Java and Maven installed. The next step is cloning the GitHub repository which contains the test sources. Most of them are located in the `kurento-test <https://github.com/Kurento/kurento-java/tree/master/kurento-integration-tests/kurento-test>`_ project, located inside of `kurento-java <https://github.com/Kurento/kurento-java/>`_. Inside this project, we need to invoke Maven to execute tests, for example as follows:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento-java.git
   cd kurento-java
   mvn \
       --projects kurento-integration-tests/kurento-test --also-make \
       -Pintegration \
       -Dgroups=org.kurento.commons.testing.IntegrationTests \
       -Dtest=WebRtcOneLoopbackTest \
       clean verify

Let's take a closer look to the Maven command:

- ``mvn [...] clean verify``: Command to execute the *clean* and *verify* goals in Maven. *clean* will ensure that old build artifacts are deleted, and *verify* involves the execution of the unit and integration tests of a Maven project.

- ``--projects kurento-integration-tests/kurento-test --also-make``: Maven options that select a single project for the goal, in this case *kurento-test*, and builds it together with any other dependency it might have.

- ``-Pintegration``: Enables the "*integration*" profile ID, as defined in the file *kurento-integration-tests/pom.xml*.

- ``-Dgroups=org.kurento.commons.testing.IntegrationTests``: The Kurento E2E test suite is divided into different `JUnit 4's categories <https://github.com/junit-team/junit4/wiki/categories>`_. This option allows to select different types of `IntegrationTests <https://github.com/Kurento/kurento-java/blob/master/kurento-commons/src/main/java/org/kurento/commons/testing/IntegrationTests.java>`_. The most used values for this group are:

   - *IntegrationTests*: Parent category for all Kurento E2E tests.
   - *SystemFunctionalTests*: To run functional tests (as defined in section before).
   - *SystemStabilityTests*: To run stability tests (as defined in section before).
   - *KurentoClientTests*: To run Java API tests (as defined in section before). If this option is used, the project should be also changed using ``--projects kurento-integration-tests/kurento-client-test``.

- ``-Dtest=WebRtcOneLoopbackTest``: Although not mandatory, it is highly recommended, to select a test or group of test using Maven's *-Dtest* parameter. Using this command we can select a test using the Java class name.

  The wildcard ``*`` can be used, and Kurento tests follow a fixed notation for their naming, so this can be used to select a group of tests. Note that it's a good idea to quote the string, to prevent unexpected shell expansions. For example:

  - ``-Dtest='WebRtc*'``: Used to execute all the functional Kurento tests for WebRTC.
  - ``-Dtest='Player*'``: Used to execute all the functional Kurento tests for player.
  - ``-Dtest='Recorder*'``: Used to execute all the functional Kurento tests for recorder.
  - ``-Dtest='Composite*'``: Used to execute all the functional Kurento tests for composite.
  - ``-Dtest='Dispatcher*'``: Used to execute all the functional Kurento tests for dispatcher.

  It's also possible to select multiple test classes with a comma (``,``), such as in ``-Dtest=TestClass1,TestClass2``.

  Finally, it is possible to select individual methods *inside* the test classes, separating them with the ``#`` symbol:

  - ``-Dtest='PlayerOnlyAudioTrackTest#testPlayerOnlyAudioTrackFileOgg*'``: Run the *PlayerOnlyAudioTrackTest.testPlayerOnlyAudioTrackFileOgg* in all its browser configurations (first Chrome, then Firefox).

  Note that the method name is given with a wildcard; this is because for most tests, the actual method name includes information about the browser which is used. Using a wildcard would run this test with both Chrome and Firefox; to choose specifically between those, specify it in the method name:

  - ``-Dtest='PlayerOnlyAudioTrackTest#testPlayerOnlyAudioTrackFileOgg[0: chrome]'``: Run *PlayerOnlyAudioTrackTest.testPlayerOnlyAudioTrackFileOgg* exclusively with the Chrome browser. Normally, Chrome is "*[0: chrome]*" and Firefox is "*[1: firefox]*".

  Other combinations are possible:

  - ``-Dtest='TestClass#testMethod1*+testMethod2*'``: Run *testMethod1* and *testMethod2* from the given test class.

An HTML report summarizing the results of a test suite executed with KTF is automatically created for Kurento tests. This report is called *report.html* and it is located by default on the *target* folder when tests are executed with Maven. The following picture shows an example of the content of this report.

.. figure:: ../images/kurento-test-report.png
   :align:   center
   :alt:     Kurento Test Framework report sample

   *Kurento Test Framework report sample*

Kurento tests are highly configurable. This configuration is done simply adding extra JVM parameters (i.e. ``-Dparameter=value``) to the previous Maven command. The following sections summarizes the main test parameters and its default values organized in different categories.

Kurento Media Server
--------------------

Kurento Media Server (KMS) is the heart of Kurento and therefore it must be properly configured in E2E tests. The following table summarizes the main options to setup KMS in these tests:

+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| **Parameter**                | **Description**                                                                                                                                                                                                                                                                                                                                                             | **Default value**                                                                                     |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *test.kms.autostart*         | Specifies if tests must start Kurento Media Server by themselves (with the method set by *test.kms.scope*), or if an external KMS service should be used instead:                                                                                                                                                                                                           | *test*                                                                                                |
|                              |                                                                                                                                                                                                                                                                                                                                                                             |                                                                                                       |
|                              | - *false*: Test must use an external KMS service, located at the URL provided by property  *kms.ws.uri*                                                                                                                                                                                                                                                                     |                                                                                                       |
|                              | - *test*: A KMS instance is automatically started before each test execution, and stopped afterwards.                                                                                                                                                                                                                                                                       |                                                                                                       |
|                              | - *testsuite*: A KMS instance is started at the beginning of the test suite execution. A "test suite" is the whole group of tests to be executed (e.g. all functional tests). KMS service is stopped after test suite execution.                                                                                                                                            |                                                                                                       |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *test.kms.scope*             | Specifies how to start KMS when it is internally managed by the test itself (``-Dtest.kms.autostart != false``):                                                                                                                                                                                                                                                            | *local*                                                                                               |
|                              |                                                                                                                                                                                                                                                                                                                                                                             |                                                                                                       |
|                              | - *local*: Try to use local KMS installation. Test will fail is no local KMS is found.                                                                                                                                                                                                                                                                                      |                                                                                                       |
|                              | - *remote*: KMS is a remote host (use *kms.login* and *kms.passwd*, or *kms.pem*, to access using SSH to the remote machine).                                                                                                                                                                                                                                               |                                                                                                       |
|                              | - *docker*: Request the docker daemon to start a KMS container based in the image specified by *test.kms.docker.image.name*. Test will fail if daemon is unable to start KMS container. In order to use this scope, a Docker server should be installed in the machine running tests. In addition, the Docker REST should be available for Docker client (used in test).    |                                                                                                       |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *test.kms.docker.image.name* | KMS docker image used to start a new docker container when KMS service is internally managed by test (``-Dtest.kms.autostart=test`` or ``testsuite``) with docker scope (``-Dtest.kms.scope=docker``). Ignored if *test.kms.autostart* is *false*. See available Docker images for KMS in `Docker Hub <https://hub.docker.com/r/kurento/kurento-media-server-dev/tags/>`__. | *kurento/kurento-media-server-dev:latest*                                                             |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.ws.uri*                 | URL of a KMS service. This property is mandatory when service is externally managed (``-Dtest.kms.autostart=false``) and ignored otherwise. Notice this URL must be reachable from Selenium nodes as well as from tests.                                                                                                                                                    | ``ws://localhost:8888/kurento``                                                                       |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.log.level*              | Debug options used to start KMS service when is internally managed by test  (``-Dtest.kms.autostart=test`` or ``testsuite``). Ignored if *test.kms.autostart* is *false*.                                                                                                                                                                                                   | ``3,Kurento*:5,kms*:5,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4`` |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.log.path*               | Path where logs from KMS will be stored. It MUST be terminated with a trailing slash (``/``).                                                                                                                                                                                                                                                                               | ``/var/log/kurento-media-server/``                                                                    |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.command*                | Shell command to start KMS.                                                                                                                                                                                                                                                                                                                                                 | ``/usr/bin/kurento-media-server``                                                                     |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.login*                  | Username to login with SSH into the machine hosting KMS.                                                                                                                                                                                                                                                                                                                    | none                                                                                                  |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.passwd*                 | Password to login with SSH into the machine hosting KMS.                                                                                                                                                                                                                                                                                                                    | none                                                                                                  |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.pem*                    | Certificate path to login with SSH into the machine hosting KMS.                                                                                                                                                                                                                                                                                                            | none                                                                                                  |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *kms.gst.plugins*            | GST plugins to be used in KMS.                                                                                                                                                                                                                                                                                                                                              | none                                                                                                  |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+
| *test.print.log*             | Print KMS logs at the end of a failed test.                                                                                                                                                                                                                                                                                                                                 | *true*                                                                                                |
+------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------+

..
   This table has been generated using https://www.tablesgenerator.com/text_tables

For example, in order to run the complete WebRTC functional test suite against a local instance KMS, the Maven command would be as follows:

.. code-block:: shell

   mvn \
       --projects kurento-integration-tests/kurento-test --also-make \
       -Pintegration \
       -Dgroups=org.kurento.commons.testing.SystemFunctionalTests \
       -Dtest=WebRtc* \
       -Dtest.kms.autostart=false \
       clean verify

In this case, an instance of KMS should be available in the machine running the tests, on the URL ``ws://localhost:8888/kurento`` (which is the default value for *kms.ws.uri*).

Browsers
--------

In order to test automatically the web application under test using Kurento, web browsers (typically Chrome or Firefox, which allow to use WebRTC) are required. The options to configure these browsers are summarized in the following table:

+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| **Parameter**               | **Description**                                                                                                                                                                                                                 | **Default value**                 |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *test.selenium.scope*       | Specifies the scope used for browsers in Selenium test scenarios:                                                                                                                                                               | *local*                           |
|                             |                                                                                                                                                                                                                                 |                                   |
|                             | - *local*: browser installed in the local machine.                                                                                                                                                                              |                                   |
|                             | - *docker*: browser in Docker container (Chrome or Firefox).                                                                                                                                                                    |                                   |
|                             | - *saucelabs*: browser in SauceLabs cloud.                                                                                                                                                                                      |                                   |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *docker.node.chrome.image*  | Docker image identifier for Chrome when browser scope is *docker*.                                                                                                                                                              | *elastestbrowsers/chrome:latest*  |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *docker.node.firefox.image* | Docker image identifier for Firefox when browser scope is *docker*.                                                                                                                                                             | *elastestbrowsers/firefox:latest* |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *test.selenium.record*      | Allow recording the browser while executing a test, and generate a video with the completely test. This feature can be activated (*true*) only if the scope for browsers is *docker*.                                           | *false*                           |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *test.config.file*          | Path to a JSON-based file with configuration keys (test scenario, see "KTF explained" section for further details). Its content is transparently managed by test infrastructure and passed to tests for configuration purposes. | *test.conf.json*                  |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *test.timezone*             | Time zone for dates in browser log traces. This feature is interesting when using Saucelabs browsers, in order to match dates from browsers with KMS. Accepted values are *GMT*, *CET*, etc.                                    | none                              |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *saucelab.user*             | User for SauceLabs                                                                                                                                                                                                              | none                              |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *saucelab.key*              | Key path for SauceLabs                                                                                                                                                                                                          | none                              |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *saucelab.idle.timeout*     | Idle time in seconds for SauceLabs requests                                                                                                                                                                                     | *120*                             |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *saucelab.command.timeout*  | Command timeout for SauceLabs requests                                                                                                                                                                                          | *300*                             |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+
| *saucelab.max.duration*     | Maximum duration for a given SauceLabs session (in seconds)                                                                                                                                                                     | 1800                              |
+-----------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-----------------------------------+

For example, in order to run the complete WebRTC functional test suite using *dockerized* browsers and recordings, the command would be as follows:

.. code-block:: shell

   mvn \
       --projects kurento-integration-tests/kurento-test --also-make \
       -Pintegration \
       -Dgroups=org.kurento.commons.testing.SystemFunctionalTests \
       -Dtest=WebRtc* \
       -Dtest.selenium.scope=docker \
       -Dtest.selenium.record=true \
       clean verify

In order to avoid wasting too much disk space, recordings of successful tests are deleted at the end of the test. For failed tests, however, recordings will be available by default on the path ``target/surefire-reports/`` (which can be changed using the property ``-Dtest.project.path``).

Web server
----------

Kurento is typically consumed using a web application. E2E tests follow this architecture, and so, a web application up and running in a web server is required. Kurento-test provides a sample web application out-of-the-box aimed to assess main Kurento features. Also, a custom web application for tests can be specified using its URL. The following table summarizes the configuration options for the test web applications.

+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| **Parameter**        | **Description**                                                                                                                                                                                                                                                                                              | **Default value** |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| *test.app.autostart* | Specifies whether test application where Selenium browsers connect must be started by test or if it is externally managed:                                                                                                                                                                                   | *testsuite*       |
|                      |                                                                                                                                                                                                                                                                                                              |                   |
|                      | - *false* : Test application is externally managed and not started by test. This is required when the web under test is already online. In this case, the URL where Selenium browsers connects is specified by the properties: *test.host*, *test.port*, *test.path* and *test.protocol*.                    |                   |
|                      | - *test* : test application is started before each test execution.                                                                                                                                                                                                                                           |                   |
|                      | - *testsuite*: Test application is started at the beginning of test execution.                                                                                                                                                                                                                               |                   |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| *test.host*          | IP address or host name of the URL where Selenium browsers will connect when test application is externally managed (``-Dtest.app.autostart=false``). Notice this address must be reachable by Selenium browsers and hence network topology between browser and test application must be taken into account. | *127.0.0.1*       |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| *test.port*          | Specifies port number where test application must bind in order to listen for browser requests.                                                                                                                                                                                                              | *7779*            |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| *test.path*          | Path of the URL where Selenium connects when test application is externally managed (``-Dtest.app.autostart=false``).                                                                                                                                                                                        | ``/``             |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| *test.protocol*      | Protocol of the URL where Selenium browsers will connect when test application is externally managed (``-Dtest.app.autostart=false``).                                                                                                                                                                       | *http*            |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+
| *test.url.timeout*   | Timeout (in seconds) to wait that web under test is available.                                                                                                                                                                                                                                               | *500*             |
+----------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------+

Fake clients
------------

In some tests (typically in performance or stability tests), another instance of KMS is used to generate what we call *fake clients*, which are WebRTC peers which are connected in a WebRTC one to many communication. The KMS used for this features (referred as *fake KMS*) is controlled with the parameters summarized in the following table:

+----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------+
| **Parameter**        | **Description**                                                                                                                                                                                                                                 | **Default value**               |
+----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------+
| *fake.kms.scope*     | This property is similar to *test.kms.scope*, except that it affects the KMS used by fake client sessions.                                                                                                                                      | *local*                         |
+----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------+
| *fake.kms.ws.uri*    | URL of a KMS service used by WebRTC clients. This property is used when service is externally managed (``-Dfake.kms.autostart=false``) and ignored otherwise. If not specified, *kms.ws.uri* is first looked at before using the default value. | ``ws://localhost:8888/kurento`` |
+----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------+
| *fake.kms.autostart* | Specifies if tests must start KMS or an external KMS service must be used for fake clients (sessions that use KMS media pipelines instead of the WebRTC stack provided by a web browser):                                                       | *false*                         |
|                      |                                                                                                                                                                                                                                                 |                                 |
|                      | - *false*: Test must use an external KMS service whose URL is provided by the property *fake.kms.ws.uri* (with *kms.ws.uri* as fallback). Test will fail if neither properties are provided.                                                    |                                 |
|                      | - *test*: KMS instance is started for before each test execution. KMS is destroyed after test execution.                                                                                                                                        |                                 |
|                      | - *testsuite*: KMS service is started at the beginning of test suite execution. KMS service is stopped after test suite execution.                                                                                                              |                                 |
|                      |                                                                                                                                                                                                                                                 |                                 |
|                      | Following properties are honored when KMS is managed by test: *fake.kms.scope*, *test.kms.docker.image.name*, *test.kms.debug*                                                                                                                  |                                 |
+----------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------+

Although available in KTF, the fake clients feature is not very used in the current tests. You can see an example in the stability test `LongStabilityCheckMemoryTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/test/java/org/kurento/test/longstability/LongStabilityCheckMemoryTest.java>`_.

Other test features
-------------------

Kurento tests can be configured in many different ways. The following table summarizes these miscellaneous features for tests.

+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| **Parameter**                | **Description**                                                                                                                                                                                                                                    | **Default value**                  |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.num.retries*           | Number of retries for failed tests                                                                                                                                                                                                                 | *1*                                |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.report*                | Path for HTML report                                                                                                                                                                                                                               | ``target/report.html``             |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.project.path*          | Path for test file output (e.g. log files, screen captures, and video recordings).                                                                                                                                                                 | ``target/surefire-reports/``       |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.workspace*             | Absolute path of working directory used by tests as temporary storage. Make sure test user has full access to this folder.                                                                                                                         | ``/tmp``                           |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.workspace.host*        | Absolute path, seen by docker agent, where directory *test.workspace* is mounted. Mandatory when scope is set to docker, as it is used by test infrastructure to share config files. This property is ignored when scope is different from docker. | *none*                             |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.docker.forcepulling*   | Force running *docker pull* to always get the latest Docker images.                                                                                                                                                                                | *true*                             |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.files.disk*            | Absolute path where test files (videos) are located.                                                                                                                                                                                               | ``/var/lib/jenkins/test-files``    |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.files.http*            | Hostname (without "http://") of a web server where test files (videos) are located.                                                                                                                                                                | *files.openvidu.io*                |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.player.url*            | URL used for playback tests. It can be anything supported by PlayerEndpoint: *file://...*, *http://...*, *rtsp://...*, etc.                                                                                                                        | *http://{test.files.http}*         |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *project.path*               | In Maven reactor projects this is the absolute path of the module where tests are located. This parameter is used by test infrastructure to place test attachments. Notice this parameter must not include a trailing ``/``.                       | ``.``                              |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *kms.generate.rtp.pts.stats* | Path where rtp/pst statistics will be stored                                                                                                                                                                                                       | ``file://WORKSPACE/testClassName`` |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *bower.kurentoclient.tag*    | Tag used by Bower to download kurento-client                                                                                                                                                                                                       | none                               |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *bower.kurentoutils.tag*     | Tag used by Bower to download kurento-utils.                                                                                                                                                                                                       | none                               |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *bower.release.url*          | URL from where JavaScript binaries (kurento-client and kurento-utils) will be downloaded. Dependencies will be gathered from Bower if this parameter is not provided.                                                                              | none                               |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.seek.repetitions*      | Number of times the tests with seek feature will be executed                                                                                                                                                                                       | *100*                              |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.num.sessions*          | Number of total sessions executed in stability tests                                                                                                                                                                                               | *50*                               |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+
| *test.screenshare.title*     | Title of the window to be shared automatically from tests                                                                                                                                                                                          | *Screen 1*                         |
+------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------------------------+

Kurento Testing Framework explained
===================================

In order to assess properly Kurento from a final user perspective, a rich suite of E2E tests has been designed and implemented. To that aim, the **Kurento Testing Framework** (KTF) has been created. KTF is a part of the Kurento project aimed to carry out end-to-end (E2E) tests for Kurento. KTF has been implemented on the top of two well-known Open Source testing frameworks: `JUnit <https://junit.org/>`_ and `Selenium <https://www.seleniumhq.org/>`_.

KTF provides high level capabilities to perform advanced automated testing for Kurento-based applications. KTF has been implemented in Java, and as usual it is hosted on GitHub, in the project `kurento-test <https://github.com/Kurento/kurento-java/tree/master/kurento-integration-tests/kurento-test>`_. KTF has been designed on the top of **JUnit 4**, providing a rich hierarchy of classes which are going to act as parent for JUnit 4 tests cases. This hierarchy is the following:

.. figure:: ../images/ktf-class-diagram.png
   :align:   center
   :alt:     Kurento Testing Framework class hierarchy

   *Kurento Testing Framework class hierarchy*

The most important classes of this diagram are the following:

- `KurentoTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/KurentoTest.java>`_: Top class of the KTF. It provides different features out-of-the-box for tests extending this class, namely:

   - Improved test lifecycle: KTF enhances the lyfecycle of JUnit 4 test cases, watching the result of tests (passed, failed). Moreover, KTF provides extra annotations to be used in different parts of the test lifecycle, such as `FailedTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/FailedTest.java>`_, `FinishedTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/FinishedTest.java>`_, `FinishedTestClass <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/FinishedTestClass.java>`_, `StartedTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/StartedTest.java>`_, `StartedTestClass <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/StartedTestClass.java>`_, or `SucceededTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/lifecycle/SucceededTest.java>`_.

   - Reporting: As introduced before, an HTML report summarizing the results of a test suite executed with KTF is automatically created for Kurento tests (*report.html*, located by default on the *target* folder when tests are executed with Maven).

   - Retries mechanism: In order to detect flaky tests, a retries mechanism is present in KTF. This mechanism allows to repeat a failed test a configurable number of times.

- `KurentoClientTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/KurentoClientTest.java>`_: It provides an instance of **Kurento Media Server** (KMS) together with a instance of a **Kurento Java Client** to control KMS. There are three options to run this KMS (see parameter *test.kms.scope*):

   - Local KMS. To use this option, it is a pre-requisite to have KMS installed in the machine running this type of tests.

   - Remote KMS. To use this option, it is a pre-requisite that KMS is installed in a remote host. If this KMS is going to be started by tests, then it is also required to have SSH access to the remote host in which KMS is installed (using parameters *kms.login* and *kms.passwd*, or providing a certificate with *kms.pem*).

   - KMS in a **Docker** container. To use this option, it is a pre-requisite to have `Docker <https://www.docker.com/>`_ installed in the machine running this type of tests.

- `BrowserTest <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/base/BrowserTest.java>`_: This class provides wrappers of `Selenium WebDriver <https://www.seleniumhq.org/projects/webdriver/>`_ instances aimed to control a group of web browsers for tests. By default, KTF allows to use **Chrome** or **Firefox** as browsers. The scope of these browsers can be configured to use:

   - Local browser, i.e. installed in the local machine.

   - Remote browser, i.e. installed in the remote machines (using Selenium Grid).

   - Docker browsers, i.e. executed in `Docker <https://www.docker.com/>`_ containers.

   - Saucelabs browsers. `Saucelabs <https://saucelabs.com/>`_ is a cloud solution for web testing. It provides a big number of browsers to be used in Selenium tests. KTF provides seamless integration with Saucelabs.

   Test scenario can be configured in *BrowserTest* tests in two different ways:

   - Programmatically using Java. Test scenario uses JUnit 4's parameterized feature. The Java class `TestScenario <https://github.com/Kurento/kurento-java/blob/master/kurento-integration-tests/kurento-test/src/main/java/org/kurento/test/config/TestScenario.java>`_ is used by KTF to configure the scenario, for example as follows:

   .. code-block:: java

      @Parameters(name = "{index}: {0}")
      public static Collection<Object[]> data() {
         TestScenario test = new TestScenario();
         test.addBrowser(BrowserConfig.BROWSER, new Browser.Builder().browserType(BrowserType.CHROME)
             .scope(BrowserScope.LOCAL).webPageType(webPageType).build());

         return Arrays.asList(new Object[][] { { test } });
      }

   - Using a JSON file. KTF allows to describe tests scenarios based on JSON notation. For each execution defined in these JSON files, the browser scope can be chosen. For example, the following example shows a test scenario in which two executions are defined. First execution defines two local browsers (identified as peer1 and peer2), Chrome and Firefox respectively. The second execution defines also two browsers, but this time browsers are located in the cloud infrastructure provided by Saucelabs.

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


Test scenario in JSON
---------------------

Test scenario consist of a list of executions, where each execution describes how many browsers must be available and their characteristics. Each browser has an unique identifier (can be any string) meaningful for the test. The following keys can be specified in a JSON test scenario in order to customize individual instances:

-  *scope*: Specifies what type of  browser infrastructure has to be used by the test execution. This value can be overridden by command line property *test.selenium.scope*.

   - *local*:  Start the browser as a local process in the same CPU where test is executed.
   - *docker*: Start browser as a docker container.
   - *saucelabs*: Start browser in SauceLabs.

- *host*: IP address or host name of URL used by the browser to execute tests. This value can be overridden by command line property *test.host*.

- *port*: Port number of the URL used by the browser to execute the test. This value can be overridden by command line property *test.port*.

- *path*: Path of the URL used by browser to execute the test. This value can be overridden by command line property *test.path*.

- *protocol*: Protocol of the URL used by browser to execute the test. This value can be overridden by command line property *test.protocol*.

- *browser*: Specifies the browser platform to be used by the test execution. Test will fail if required browser is not found.

- *saucelabsUser*: SauceLabs user. This property is mandatory for SauceLabs scope and ignored otherwise. Its value can be overridden by command line property *saucelab.user*.

- *saucelabsKey*: SauceLabs key. This property is mandatory for SauceLabs scope and ignored otherwise. Its value can be overridden by command line property *saucelab.key*.

- *version*: Version of browser to be used when test is executed in SauceLabs infrastructure. Test will fail if requested version is not found.



TO-DO
-----

Rename:

- test.kms.docker.image.name -> test.docker.image.kms
- docker.node.chrome.image -> test.docker.image.chrome
- docker.node.firefox.image -> test.docker.image.firefox
