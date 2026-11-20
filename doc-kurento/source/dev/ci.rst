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
- If any other test fails, or a flaky test fails in all of its runs, the build fails as usual. No test is disabled by the retry mechanism; the cases skipped by default are listed below.
- The script can be tuned with the environment variables ``FLAKY_TEST_REGEX`` (names of the tests that can be retried, default ``^test_webrtc_?endpoint$``, i.e. ``test_webrtcendpoint`` and ``test_webrtc_endpoint``) and ``FLAKY_TEST_MAX_ATTEMPTS`` (default ``3``; use ``1`` to disable the retries). The variable ``FLAKY_TEST_TIMEOUT`` (default ``1200``) sets a time limit in seconds for every test of ``kurento-module-elements`` (``ctest --timeout``), in the first run and in the retries. All three variables are passed by the job from its environment to the build container.
- As usual, ``DEB_BUILD_OPTIONS=nocheck`` skips the tests altogether (debhelper skips the whole ``override_dh_auto_test`` target).

In ``kurento-module-elements`` (currently the only module with this override), tests that hang are killed by the time limit and reported as failed (``Timeout``), instead of blocking the job until the CI timeout expires.

The IPv6 test cases of ``test_webrtc_endpoint`` (``*_ipv6``, in ``server/module-elements/tests/server/webRtcEndpoint.cpp``) need an IPv6 network. They detect at runtime whether the host has a usable IPv6 address (not only loopback ``::1`` or link-local ``fe80::``), and if not, they are skipped with a ``SKIPPED: <test>: this host has no usable IPv6 address`` message in the log. Docker containers only get IPv6 if the Docker daemon is configured for it (``ipv6: true`` in ``daemon.json``); when there is IPv6, the tests run as usual. Without this, in a container with no IPv6 these cases fail with ICE/DTLS timeouts, and the failed cases leave endpoints alive, which can block the test process at exit; the time limit above is the safety net for that situation.

The test case ``check_data_channel`` of ``test_webrtc_endpoint`` is skipped (with a ``SKIPPED: check_data_channel: known issue...`` message) because of a known issue: creating a data channel with the same stream id right after closing the previous one sometimes never completes (this failure is independent of IPv6). To run it anyway, set ``KURENTO_TEST_ENABLE_DATA_CHANNEL_REOPEN=1`` in the environment of the test. Its waits are bounded and the endpoints are released even when it fails, so a failure does not leave the test process hung.

This is a temporary measure: **the retry must be removed (delete the script and the ``override_dh_auto_test`` rule) once the underlying crash is fixed.**

Besides, the build container is started with ``--cap-add=SYS_NICE``. Without it, the x265 encoder (through libnuma) calls ``set_mempolicy()``, which Docker's default security profile denies, and the test logs get flooded with ``set_mempolicy: Operation not permitted`` messages.


Runner labels
=============

The GitHub Actions workflows run on *self-hosted* runners, selected by labels in the ``runs-on`` field of each job (labels are case-insensitive). Every runner must have these labels:

- ``self-hosted``, ``Linux`` and ``X64`` or ``ARM64``: added automatically by GitHub according to the OS and architecture of the machine.
- ``amd64`` or ``arm64``: the CPU architecture, as used in the ``jobArch`` matrix of the workflows. Exactly one of them. An ARM64 runner already satisfies ``arm64`` through its automatic ``ARM64`` label, but x64 runners must be given the custom label ``amd64`` manually.
- The ``runnerGroup`` label, which is a normal label (not a GitHub runner group). Its default value is ``Linux``, which GitHub already adds.

Jobs that build for a given architecture (the ``jobArch`` matrix of the *server-buildpackage*, *server-buildpackage-rust*, *server-deploy-docker* and ``docker-*`` workflows) use ``runs-on: ["self-hosted", <runnerGroup>, <jobArch>]``. An ``amd64`` build only runs on runners labeled ``amd64``, and an ``arm64`` build stays queued until a runner labeled ``arm64`` is available; it never falls back to an ``amd64`` runner.

Jobs that do not depend on an architecture (Debian repository deployment with *aptly*, Java and JavaScript clients, documentation, AWS deployment) are pinned to ``amd64`` runners, so they never end up by accident in an ``arm64`` runner.

Until ``arm64`` runners are available, the default value of the ``jobArchs`` input is ``["amd64"]``. **Do not** add ``arm64`` to ``jobArchs`` while there are no ``arm64`` runners: the ``needs`` dependencies of ``server.parent.yaml`` make the following jobs wait for all the matrix jobs, so the queued ``arm64`` job would block the ``amd64`` ones too, until GitHub cancels it after 24 hours (and where jobs run with ``max-parallel: 1``, the order of the architectures matters). Once ``arm64`` runners exist, set ``jobArchs`` to ``["amd64", "arm64"]`` when launching the workflow, or restore that value as the ``default`` in ``server.parent.yaml`` and ``docker-*.yaml``.

An ``amd64``-only run publishes the Debian repositories without ``arm64`` packages. For Docker images, the tag without architecture suffix (e.g. ``dev-pr-<N>``, ``latest``) is created pointing only to the architectures that were requested in ``jobArchs``, so an ``amd64``-only run never mixes in older ``-arm64`` images.

The ARM64 emulation (QEMU, ``docker run --platform``) is kept in the scripts. It is harmless on a native runner, where the requested platform matches the host. Note that C/C++ tests are skipped in arm64 builds (``DEB_BUILD_OPTIONS=nocheck``).



Docker images published by CI
=============================

The *server-deploy-docker* workflow (``server-deploy-docker.child.yaml``) publishes to Docker Hub, for each architecture in ``jobArchs``, these images of ``kurento/kurento-media-server``: the main image (``<tag>-<arch>``), the one with the extra modules (``kurento-media-server-extras``) and, with ``buildAsan`` enabled, the image with AddressSanitizer (tag suffix ``-asan``). The tags without architecture suffix are multi-arch manifests that point to the requested architectures.

The AddressSanitizer image is built in its own job (``build-asan``), after the ``build`` job of the main and extras images, and not as a step of it. This image compiles Kurento from source (``docker/kurento-media-server-asan/``), which takes much longer than the rest (more than 16 minutes on amd64 and more than 25 on emulated arm64, and more on loaded machines). When it shared the job with the main images, the 30 minutes time limit of the job cut it, and the whole run was marked as cancelled even though the packages and the main images were already published. Now ``build-asan`` is a separate job with ``timeout-minutes: 120``, and its step "Run job script (AddressSanitizer)" has its own limit (``timeout-minutes: 100``) and ``continue-on-error: true``. The reason is that a job which reaches its time limit is concluded by GitHub as *cancelled*, and it is not documented that ``continue-on-error`` of a job neutralizes that; a step that reaches its limit is just marked as failed, so the job never gets to its own limit. If the ASan image fails or runs out of time, that step is shown as failed (a warning), the job and the run stay in success, and the ``-asan`` images are not published or updated: check that step after a run to know if they were.

Other things to keep in mind:

- ``build-asan`` is skipped entirely if ``build`` fails in any architecture, also in the architectures that were fine.
- The architectures run one after another (``max-parallel: 1``), so in the worst case the ASan job adds up to 100 minutes per architecture to the duration of the run, holding the self-hosted runners.
- The multi-arch manifest ``<tag>-asan`` is created by the leg that finishes last, from the ``<tag>-asan-<arch>`` images that exist at that moment. If the ASan build of one architecture fails, an older ``-asan-<arch>`` image from a previous run can remain in the manifest, mixed with the new image of the other architecture.
