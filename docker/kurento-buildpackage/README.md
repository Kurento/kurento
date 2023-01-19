# kurento-buildpackage

Image source: [Dockerfile](https://github.com/Kurento/kurento/blob/main/docker/kurento-buildpackage/Dockerfile).

This Docker image is used to generate Debian packages from Kurento projects.
It runs [kurento-buildpackage](https://github.com/Kurento/kurento/blob/main/ci-scripts/kurento-buildpackage.sh) from a properly configured system.



## Example usage

* To build the current development version of [kurento-module-core](https://github.com/Kurento/kurento/tree/main/server/module-core), with its dependencies downloaded from the prebuilt packages repository:

  ```
  git clone https://github.com/Kurento/kurento.git
  cd kurento/server/module-core/
  docker run --rm \
      --mount type=bind,src="$PWD",dst=/hostdir \
      kurento/kurento-buildpackage:focal \
      --install-kurento nightly
  ```

* Same as above, but building against a specific released version of the dependencies, e.g. **7.0.0**:

  Change `--install-kurento nightly` to `--install-kurento 7.0.0`.

* For more ways to use this tool, call it with `--help` and read the output that gets printed.



## Help output

```
$ docker run --rm kurento/kurento-buildpackage:focal --help

Kurento packaging script for Debian/Ubuntu.

This script is used to build all Kurento Media Server modules, and generate
Debian/Ubuntu package files (.deb) from them. It will automatically install
all required dependencies with `apt-get`, then build the project.

This script must be called from within a Git repository.



Arguments
---------

--install-kurento <KurentoVersion>

  Install dependencies that are required to build the package, using the
  Kurento package repository for those packages that need it. This is useful
  for quickly building a specific component of Kurento (e.g. "kurento-module-core")
  without also having to build all of its dependencies.

  <KurentoVersion> indicates which Kurento repo should be used to download
  packages from. E.g.: "7.0.0", or "dev" for nightly builds. Typically, you
  will provide an actual version number when also using '--release', and just
  use "dev" otherwise.

  The appropriate Kurento repository line for apt-get must be already present
  in some ".list" file under /etc/apt/. To have this script adding the
  required line automatically for you, use '--apt-add-repo'.

  Optional. Default: Disabled.
  See also:
    --install-files
    --apt-add-repo

--install-files [FilesDir]

  Install specific dependency files that are required to build the package.

  [FilesDir] is optional, and defaults to the current working directory. It
  tells this tool where all '.deb' files are located, to be installed.

  This argument is useful during incremental builds where dependencies have
  been built previously but are still not available to download with
  `apt-get`, maybe as a product of previous jobs in a CI pipeline.

  '--install-files' can be used together with '--install-kurento'. If none of
  the '--install-*' arguments are provided, all non-system dependencies are
  expected to be already installed.

  Optional. Default: Disabled.
  See also:
    --install-kurento

--srcdir <SrcDir>

  Specifies in which sub-directory the script should work. If not specified,
  all operations will be done in the current directory where the script has
  been called.

  The <SrcDir> MUST contain a 'debian/' directory with all Debian files,
  which are used to define how to build the project and generate packages.

  This argument is useful for Git projects that contain submodules. Running
  directly from a submodule directory might cause some problems if the
  command git-buildpackage is not able to identify the submodule as a proper
  Git repository.

  Optional. Default: Current working directory.

--dstdir <DstDir>

  Specifies where the resulting Debian package files ('*.deb') should be
  placed after the build finishes.

  Optional. Default: Current working directory.

--allow-dirty

  Allows building packages from a working directory where there are unstaged
  and/or uncommited source code changes. If this option is not given, the
  working directory must be clean.

  NOTE: This tells `dpkg-buildpackage` to skip calling `dpkg-source` and
  build a Binary-only package. It makes easier creating a test package, but
  in the long run the objective is to create oficially valid packages which
  comply with Debian/Ubuntu's policies, so this option should not be used for
  final release builds.

  Optional. Default: Disabled.

--release

  Build packages intended for Release. If this option is not given, packages
  are built as nightly snapshots.

  Optional. Default: Disabled.

--timestamp <Timestamp>

  Apply the provided timestamp instead of using the date and time this script
  is being run.

  <Timestamp> must be a decimal number. Ideally, it represents some date and
  time when the build was done. It can also be any arbitrary number.

  Optional. Default: Current date and time, as given by the command
  `date --utc +%Y%m%d%H%M%S`.

--apt-add-repo

  Edit the system config to add a Kurento repository line for apt-get.

  This adds or edits the file "/etc/apt/sources.list.d/kurento.list" to make
  sure that `apt-get` will be able to download and install all required
  packages from the Kurento repository, if the line wasn't already there.

  To use this argument, '--install-kurento' must be used too.

--apt-proxy <ProxyUrl>

  Use the given HTTP proxy for apt-get. This can be useful in environments
  where such a proxy is set up, in order to save on data transfer costs from
  official system repositories.

  <ProxyUrl> is set to Apt option "Acquire::http::Proxy".

  Doc: https://manpages.ubuntu.com/manpages/man1/apt-transport-http.1.html



Dependency tree
---------------

* git-buildpackage
  - Python 3 (pip, setuptools, wheel)
  - debuild (package 'devscripts')
    - dpkg-buildpackage (package 'dpkg-dev')
    - lintian
  - git
    - openssh-client (for Git SSH access)
* mk-build-deps (package 'devscripts')
  - equivs
* nproc (package 'coreutils')
* realpath (package 'coreutils')


Dependency install
------------------

sudo apt-get update ; sudo apt-get install --no-install-recommends \
    python3 python3-pip python3-setuptools python3-wheel \
    devscripts \
    dpkg-dev \
    lintian \
    git \
    openssh-client \
    equivs \
    coreutils
sudo python3 -m pip install --upgrade gbp
```
