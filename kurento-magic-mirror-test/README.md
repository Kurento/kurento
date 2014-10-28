[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

kurento-magic-mirror
====================
Kurento Java Tutorial 2: WebRTC in loopback with filter (magic mirror).


What is Kurento
---------------
Kurento provides an open platform for video processing and streaming
based on standards.

This platform has several APIs and components which provide solutions
to the requirements of multimedia content application developers.
These include:

  * Kurento Media Server (KMS). A full featured media server providing
    the capability to create and manage dynamic multimedia pipelines.
  * Kurento Clients. Libraries to create applications with media
    capabilities. Kurento provides libraries for Java, browser JavaScript,
    and Node.js.


Integration Test
----------------
This application includes an integration test to check its correctness. The
requirements of this test are:

  * Kurento Media Server. It must be installed and running an instance of KMS
    in the machine running the test. This can be done as follows:

		sudo add-apt-repository ppa:kurento/kurento
		wget -O - http://ubuntu.kurento.org/kurento.gpg.key | sudo apt-key add -
		sudo apt-get update
		sudo apt-get install kurento-media-server

    For more information please read the [Kurento documentation].

  * Chrome. It must be installed an Google Chrome browser in the machine running
    the test. In addition, it is recommended to use its latest stable version.
    In a 64bit Ubuntu machine, it can be installed by means of:

		wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
		sudo dpkg -i google-chrome*.deb

  * ChromeDriver. This tests uses [Selenium WebDriver] to open a Chrome browser
    and this way perform an automated assessment of the application under test.
    To reach Chrome, Selenium WebDriver needs a binary file called "ChromeDriver".
    You need to get the specific binary for your operative system in the
    [ChromeDriver download page]. Unzip the downloaded package and store the binary
    in your local hard disk.

If you meet these three requirements, you are able to carry out the test. It has
been implemented as an integration test using Maven. Therefore, to run it from the
command line you should execute the following command:

	mvn verify -Dwebdriver.chrome.driver=<absolute-path-to-chromedriver>

For instance, in an Linux 64bit machine:

	mvn verify -Dwebdriver.chrome.driver=/opt/chromedriver/2.10/linux64/chromedriver

If your KMS is not located in the local machine (or it is listening in a different port
that the default 8888), the KMS WebSocket can be changed using the argument "kms.ws.uri",
as follows:

	mvn verify -Dwebdriver.chrome.driver=<absolute-path-to-chromedriver> -Dkms.ws.uri=<ws://host:port/kurento>

For instance:

	mvn verify -Dwebdriver.chrome.driver=/opt/chromedriver/2.10/linux64/chromedriver -Dkms.ws.uri=ws://localhost:8888/kurento


Source
------
The source code of this project can be cloned from the [GitHub repository].
Code for other Kurento projects can be found in the [GitHub Kurento group].


News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].


[ChromeDriver download page]: http://chromedriver.storage.googleapis.com/index.html
[Kurento documentation]: http://www.kurento.org/documentation
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[GitHub repository]: https://github.com/Kurento/kurento-tutorial-java
[GitHub Kurento group]: https://github.com/kurento
[Selenium WebDriver]: http://docs.seleniumhq.org/projects/webdriver/
[website]: http://kurento.org
