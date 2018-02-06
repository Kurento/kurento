Development utility scripts
===========================

These scripts can be useful for developers wanting a quick way of (un)installing all packages related to Kurento Media Server.

Description of each script:

- `kurento-git-clone-all.sh` clones all relevant Git repositories from Kurento.

- `kurento-install-development.sh` installs all packages involved in development of *Kurento itself*. This includes all development libraries, tools, packages from forked repositories, and also optional packages such as those including debugging symbols.

- `kurento-install-packaging.sh` installs all tools required to use the script [compile_project.py](https://github.com/Kurento/adm-scripts/blob/master/kms/compile_project.py) to generate Debian packages from any of the Kurento repositories.

- `kurento-repo-*.sh` are a set of scripts that configure the Kurento package repositories for usage with *apt-get*.

- `kurento-uninstall-all.sh` can be used to quickly uninstall all packages related to KMS and its development. This includes every package that might get installed from a previous call to `kurento-install-development.sh`, or from older installations of Kurento. **Warning**: it uses the flag `--yes`, so no confirmation will be asked before proceeding; this could end up uninstalling half of your system if you don't know what you're doing! (but it works fine in a clean Ubuntu installation).

- `aptly_management` At the end of the release pipeline this script will upload and publish the new generated packages to our Release repository.

- `aptly_dev_management` is in charge of the dev branch in out Debian repository.

