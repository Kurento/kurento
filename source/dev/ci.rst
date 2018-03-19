======================
Continuous Integration
======================

We have two types of repositories containing Debian packages: either Release or Nightly builds of the KMS Main Repositories and KMS Fork Repositories. Each of these types of repos are made available for the currently supported Ubuntu editions, which as of this writing are Ubuntu 14.04 (Trusty) and Ubuntu 16.04 (Xenial).

After some exploration of the different options we had, in the end we settled on the option to have self-contained repos, where all Kurento packages are stored and no dependencies with additional repositories are needed (*).

There is an independent repository for each released version of Kurento, and one single repository for nightly builds:

- Repositories for Ubuntu 14.04 (Trusty):

   - Release: ``deb [arch=amd64] http://ubuntu.openvidu.io/<Version> trusty kms6``
   - Nightly: ``deb [arch=amd64] http://ubuntu.openvidu.io/dev trusty kms6``

- Repositories for Ubuntu 16.04 (Xenial):

   - Release: ``deb [arch=amd64] http://ubuntu.openvidu.io/<Version> xenial kms6``
   - Nightly: ``deb [arch=amd64] http://ubuntu.openvidu.io/dev xenial kms6``

We also have several Continuous-Integration (*CI*) jobs such that every time a patch is accepted in Git's ``master`` branch, a new package build of that repository is generated, and uploaded to the nightly repositories.

All scripts used by CI are stored in the Git repo `adm-scripts <https://github.com/Kurento/adm-scripts>`__.
