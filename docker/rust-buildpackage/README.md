# kurento-buildpackage

Image source: [Dockerfile](https://github.com/Kurento/kurento/blob/main/docker/rust-buildpackage/Dockerfile).

This Docker image is used to generate Debian packages from Kurento projects.
It runs [rust-buildpackage](https://github.com/Kurento/kurento/blob/main/ci-scripts/rust-buildpackage.sh) from a properly configured system.



## Example usage

* To build the current development version of [kurento-module-core](https://github.com/Kurento/kurento/tree/main/server/module-core), with its dependencies downloaded from the prebuilt packages repository:

  ```
  git clone https://github.com/Kurento/kurento.git
  cd kurento/server/module-core/
  docker run --rm \
      --mount type=bind,src="$PWD",dst=/hostdir \
      --mount type=bind,src=kurento/ci-scripts,dst=/ci-scripts
      kurento/rust-buildpackage:noble \
      --install-file /packages
  ```

* For more ways to use this tool, call it with `--help` and read the output that gets printed.



## Help output

```
$ docker run --rm kurento/rust-buildpackage:noble --help

Kurento packaging script for Debian/Ubuntu.

Rust packaging script for Debian/Ubuntu.

This script is used to build Rust gstreamer plugins dependencies, and generate
Debian/Ubuntu package files (.deb) from them. It will automatically install
all required dependencies with `apt-get`, then build the project.

If running on a Git repository, information about the current commit will be
added to the resulting version number (for development builds).



Arguments
---------

--dstdir <DstDir>

  Specifies where the resulting Debian package files ('*.deb') should be
  placed after the build finishes.

  Optional. Default: Current working directory.

--release

  Build packages intended for Release. If this option is not given, packages
  are built as development snapshots.

  Optional. Default: Disabled.

--timestamp <Timestamp>

  Apply the provided timestamp instead of using the date and time this script
  is being run.

  <Timestamp> must be a decimal number. Ideally, it represents some date and
  time when the build was done. It can also be any arbitrary number.

  Optional. Default: Current date and time, as given by the command
  `date --utc +%Y%m%d%H%M%S`.

--apt-proxy <ProxyUrl>

  Use the given HTTP proxy for apt-get. This can be useful in environments
  where such a proxy is set up, in order to save on data transfer costs from
  official system repositories.

  <ProxyUrl> is set to Apt option "Acquire::http::Proxy".

  Doc: https://manpages.ubuntu.com/manpages/man1/apt-transport-http.1.html

--platform <platform specification>

  Sets the target binary platform to use (e.g x86_64-linux-gnu or arm64-linux-gnu)

  <platform specification> is set by default to x86_64-linux-gnu

--package <package>

   Sets the package in the module that it is intended to be built

