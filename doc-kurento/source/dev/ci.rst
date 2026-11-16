======================
Continuous Integration
======================

We have two types of repositories containing Debian packages: either Release or Nightly builds of the Kurento Main Repositories and Kurento Fork Repositories.

After some exploration of the different options we had, in the end we settled on the option to have self-contained repos, where all Kurento packages are stored and no dependencies with additional repositories are needed.

There is an independent repository for each released version of Kurento, and one single repository for nightly builds:

- Release: ``deb http://ubuntu.openvidu.io/<KurentoVersion> <UbuntuCodename> main``
- Nightly: ``deb http://ubuntu.openvidu.io/dev <UbuntuCodename> main``

Here, *<KurentoVersion>* is any of the released versions (such as *7.0.0*) and *<UbuntuCodename>* is the name of each supported Ubuntu version (e.g. "focal").

We also have several Continuous-Integration (*CI*) jobs such that new nightly packages can be built from each Git repository's *main* branch, to be then uploaded to the nightly repositories.

All scripts used by CI are stored in the ``ci-scripts/`` subdir of the Kurento git repo: https://github.com/Kurento/kurento.



Flaky tests in package builds
=============================

The Debian package builds (``ci-scripts/ci_job_package_debian.sh``) run the C/C++ test suite of each module during ``dh_auto_test``. The test program ``test_webrtcendpoint`` (module *kurento-module-elements*) is currently known to crash at random, in about 1 of every 3 runs, even on unmodified code: the crashes come from threads that use OpenSSL (DTLS) and show up as ``SIGSEGV`` or ``SIGABRT`` in different test cases each time.

Until this bug is fixed, the package build retries this test in a bounded way, so that a single random crash doesn't break the whole build:

- The override of ``dh_auto_test`` in ``server/module-elements/debian/rules`` calls ``server/module-elements/debian/dh_auto_test_retry.sh``.
- The full test suite is run normally the first time. If tests fail, and **all** the failed tests are known flaky tests, only those are run again, up to 3 runs in total.
- Every retry is visible in the job log, with lines starting with ``FLAKY-TEST-RETRY``. A test that fails once and then passes is reported with a ``FLAKY-TEST-PASSED-ON-RETRY`` line, so the frequency of the crashes can still be measured by searching the logs.
- If any other test fails, or a flaky test fails in all of its runs, the build fails as usual. No test is disabled.
- The script can be tuned with the environment variables ``FLAKY_TEST_REGEX`` (names of the tests that can be retried, default ``^test_webrtcendpoint$``) and ``FLAKY_TEST_MAX_ATTEMPTS`` (default ``3``; use ``1`` to disable the retries). The job passes both variables from its environment to the build container.
- As usual, ``DEB_BUILD_OPTIONS=nocheck`` skips the tests altogether (debhelper skips the whole ``override_dh_auto_test`` target).

This is a temporary measure: **the retry must be removed (delete the script and the ``override_dh_auto_test`` rule) once the underlying crash is fixed.**

Besides, the build container is started with ``--cap-add=SYS_NICE``. Without it, the x265 encoder (through libnuma) calls ``set_mempolicy()``, which Docker's default security profile denies, and the test logs get flooded with ``set_mempolicy: Operation not permitted`` messages.
