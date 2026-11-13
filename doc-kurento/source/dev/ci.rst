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



Runner labels
=============

The GitHub Actions workflows run on *self-hosted* runners, selected by labels in the ``runs-on`` field of each job. Every runner must have these labels:

- ``self-hosted``: added automatically by GitHub.
- ``Linux``: the *runner group* label. It is the default value of the ``runnerGroup`` input of the workflows.
- ``amd64`` or ``arm64``: the CPU architecture of the machine where the runner is installed. Exactly one of them.

Jobs that build for a given architecture (the ``jobArch`` matrix of the *server-buildpackage*, *server-buildpackage-rust*, *server-deploy-docker* and ``docker-*`` workflows) use ``runs-on: ["self-hosted", <runnerGroup>, <jobArch>]``. An ``amd64`` build only runs on runners labeled ``amd64``, and an ``arm64`` build stays queued until a runner labeled ``arm64`` is available; it never falls back to an ``amd64`` runner.

Jobs that do not depend on an architecture (Debian repository deployment with *aptly*, Java and JavaScript clients, documentation, AWS deployment) are pinned to ``amd64`` runners, so they never end up by accident in an ``arm64`` runner.

Until ``arm64`` runners are available, the default value of the ``jobArchs`` input is ``["amd64"]``. To build for ARM64 once there are runners with the ``arm64`` label, set ``jobArchs`` to ``["amd64", "arm64"]`` when launching the workflow, or restore that value as the ``default`` in ``server.parent.yaml`` and ``docker-*.yaml``.

The ARM64 emulation (QEMU, ``docker run --platform``) is kept in the scripts. It is harmless on a native runner, where the requested platform matches the host. Note that C/C++ tests are skipped in arm64 builds (``DEB_BUILD_OPTIONS=nocheck``).
