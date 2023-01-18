kurento-one2one-call-test
=========================

Test for Kurento Java Tutorial: WebRTC one to one video call.

Running this test
-----------------

This application includes an integration test. The requirements of this test are:

  * Kurento Media Server. It must be installed and running an instance of KMS
    in the machine running the test. For further information please visit the
    official [KMS installation guide].

  * Chrome. It must be installed an Google Chrome browser in the machine running
    the test. In addition, it is recommended to use its latest stable version.
    In a 64bit Ubuntu machine, it can be installed by means of:

		wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
		sudo dpkg -i google-chrome*.deb

This test has been implemented as an integration test using Maven. To run it from the
command line you should execute the following command:

	mvn verify

If your KMS is not located in the local machine (or it is listening in a different port
that the default 8888), the KMS WebSocket can be changed using the argument "kms.ws.uri",
as follows:

	mvn verify -Dkms.ws.uri=<ws://host:port/kurento>

