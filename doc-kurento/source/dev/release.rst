==================
Release Procedures
==================

.. contents:: Table of Contents



Introduction
============

This document describes all release procedures that apply to each one of the modules that are part of the Kurento project. The main form of categorization is by technology type: C/C++ based modules, Java modules, JavaScript modules, and others.


.. _dev-release-general:

General considerations
----------------------

* Lists of projects in this document are sorted according to the repository lists given in :ref:`dev-code-repos`.

* During development, Kurento projects will have the future version number, followed by a development suffix:

  - ``-SNAPSHOT`` in Java (Maven) projects. Example: ``1.0.0-SNAPSHOT``.
  - ``-dev`` in C/C++ (CMake) and JavaScript projects. Example: ``1.0.0-dev``.

  These suffixes must be removed for release, and then added again to start a new development cycle.

* The release version number doesn't need to match the one that had been in use during development. For example, after ``1.0.1-dev``, maybe enough features had been added that it gets released as ``1.1.0`` instead of ``1.0.1``. This is something that will be decided at the time of each release.

* Git tags are created and named with the version number of each release. Example: ``1.0.0``. No superfluous prefix is used, such as ``v``.

* If hotfix branches are needed, they will use ``x`` as placeholder for the unspecified number. For example:

  - A support branch for the ``1.1`` minor release would be called ``1.1.x``.
  - A support branch for the whole ``1`` major release would be called ``1.x``.

* Contrary to the project version, Debian package versions don't contain development suffixes, and should always be of the form ``1.0.0-1kurento1``:

  - The first part (*1.0.0*) is the project's **base version number**.

  - The second part (*1kurento1*) is the **Debian package revision**:

    - The number prefix indicates the version relative to other same-name packages provided by the base system.

    - The number suffix means the amount of times that the package itself has been repackaged and republished. *1* means that this is the *first* time a given project version was packaged. If it was *1kurento3*, it would mean that's the third time the same version has been repackaged.

    **Example**:

    A new version *1.0.0* is released for the first time. The full Debian package version will be: *1.0.0-1kurento1*. Later, you realize the package doesn't install correctly in some machines, because of a bug in the package's post-install script. It's time to fix the mistake and republish! The software's source code itself has not changed at all, only the packaging files (in ``/debian/`` dir); thus, the *base version* will remain *1.0.0*, and only the *Debian revision* needs to change. The new package's full version will be *1.0.0-1kurento2*.

  Please check the `Debian Policy Manual`_ and `this Ask Ubuntu answer`_ for more information about package versions.

* Kurento uses `Semantic Versioning`_. Whenever you need to decide what is going to be the *final release version* for a new release, try to follow the SemVer guidelines:

  .. code-block:: text

     Given a version number MAJOR.MINOR.PATCH, increment the:

     1. MAJOR version when making backwards-incompatible or breaking API changes.
     2. MINOR version when adding new functionality in a backwards-compatible manner.
     3. PATCH version when making backwards-compatible bug fixes.

  Please refer to https://semver.org/ for more information.

  **Example**:

  If the last release was **1.0.0**, then the next development version would be **1.0.1-dev** (or *1.0.1-SNAPSHOT* for Java components).

  Some weeks later, the time comes to release this new development. If the new code only includes bug fixes and patches, then the version number *1.0.1* is already good. However, if some new features had been added, this new code should be released with version number *1.1.0*. The Debian package version is reset accordingly, so the full version is **1.1.0-1kurento1**.



.. note::

   Made a mistake? Don't panic!

   Do not be afraid of applying some Git magic to solve mistakes during the release process. Here are some which can be useful:

   - How to remove a release tag?

     - Remove the local tag:

       .. code-block:: shell

          git tag --delete <TagName>

     - Remove the remote tag:

       .. code-block:: shell

          git push --delete origin <TagName>

   - How to push just a local tag?

     .. code-block:: shell

        git push origin <TagName>

   - How to amend a commit and push it again?

     See: https://www.atlassian.com/git/tutorials/rewriting-history#git-commit--amend

     .. code-block:: shell

        # <Remove Tag>
        # <Amend>
        # <Create Tag>
        git push --force origin <TagName>

     Note that the **main** branch in GitHub is a protected branch. This means force-pushing is disallowed, to avoid breaking the git trees of anyone who has this repository cloned.



Release order
=============

First, the C/C++ parts of the code are built, Debian packages are created, and everything is left ready for deployment in an Apt repository (for *apt-get*) managed by `Aptly`_.

Before Kurento Media Server itself, all required forks and libraries must be built and installed:

* `libsrtp <https://github.com/Kurento/libsrtp>`__
* `openh264 <https://github.com/Kurento/openh264>`__
* `openh264-gst-plugin <https://github.com/Kurento/openh264-gst-plugin>`__
* `gst-plugins-good <https://github.com/Kurento/gst-plugins-good>`__
* `libnice <https://github.com/Kurento/libnice>`__

The main :ref:`dev-release-media-server` modules should be built in this order:

* ``server/module-creator``
* ``server/cmake-utils``
* ``server/jsonrpc``
* ``server/module-core``
* ``server/module-elements``
* ``server/module-filters``
* ``server/media-server``

And the example Kurento modules, which depend on Kurento's *core*, *elements*, and *filters*, can be built now:

* ``server/module-examples/chroma``

(NOTE: Build disabled on Ubuntu >= 20.04 due to breaking changes in OpenCV 4.0)

* ``server/module-examples/crowddetector``
* ``server/module-examples/datachannelexample``
* ``server/module-examples/markerdetector``
* ``server/module-examples/platedetector``
* ``server/module-examples/pointerdetector``

With this, the Media Server part of Kurento is built and ready for use. This includes an JSON-RPC server that listens for connections and speaks the :doc:`/features/kurento_protocol`.

To make life easier for application developers, there is a Java and a JavaScript client SDK that implements the RPC protocol. These are libraries that get auto-generated from each of the Kurento modules. See :ref:`dev-release-java` and :ref:`dev-release-javascript`.

Java release order:

* ``server/module-creator`` (`org.kurento.kurento-module-creator <https://search.maven.org/artifact/org.kurento/kurento-module-creator>`__)
* ``clients/java/maven-plugin`` (`org.kurento.kurento-maven-plugin <https://search.maven.org/artifact/org.kurento/kurento-maven-plugin>`__)
* ``clients/java/qa-pom`` (`org.kurento.kurento-qa-pom <https://search.maven.org/artifact/org.kurento/kurento-qa-pom>`__)

* ``server/module-core`` (`org.kurento.kms-api-core <https://search.maven.org/artifact/org.kurento/kms-api-core>`__)
* ``server/module-elements`` (`org.kurento.kms-api-elements <https://search.maven.org/artifact/org.kurento/kms-api-elements>`__)
* ``server/module-filters`` (`org.kurento.kms-api-filters <https://search.maven.org/artifact/org.kurento/kms-api-filters>`__)

* ``clients/java`` (`org.kurento.kurento-java <https://search.maven.org/artifact/org.kurento/kurento-java>`__, including `org.kurento.kurento-client <https://search.maven.org/artifact/org.kurento/kurento-client>`__)

After *kurento-client* is done, the client code for example Kurento modules can be built:

* ``server/module-examples/chroma`` (`org.kurento.module.chroma <https://search.maven.org/artifact/org.kurento.module/chroma>`__)
* ``server/module-examples/crowddetector`` (`org.kurento.module.crowddetector <https://search.maven.org/artifact/org.kurento.module/crowddetector>`__)
* ``server/module-examples/datachannelexample`` (`org.kurento.module.datachannelexample <https://search.maven.org/artifact/org.kurento.module/datachannelexample>`__)
* ``server/module-examples/markerdetector`` (`org.kurento.module.markerdetector <https://search.maven.org/artifact/org.kurento.module/markerdetector>`__)
* ``server/module-examples/platedetector`` (`org.kurento.module.platedetector <https://search.maven.org/artifact/org.kurento.module/platedetector>`__)
* ``server/module-examples/pointerdetector`` (`org.kurento.module.pointerdetector <https://search.maven.org/artifact/org.kurento.module/pointerdetector>`__)

Now, the Kurento testing packages (which depend on some of the example modules). *kurento-utils-js* library must also be built at this stage, because it is a dependency of *kurento-test*:

* ``browser/kurento-utils-js`` (`kurento-utils <https://www.npmjs.com/package/kurento-utils>`__)
* ``test/integration`` (`org.kurento.kurento-integration-tests <https://search.maven.org/artifact/org.kurento/kurento-integration-tests>`__, including `org.kurento.kurento-test <https://search.maven.org/artifact/org.kurento/kurento-test>`__)

And lastly, the tutorials (which depend on the example modules):

* ``tutorials/java`` (`org.kurento.tutorial.kurento-tutorial <https://search.maven.org/artifact/org.kurento.tutorial/kurento-tutorial>`__, including `org.kurento.tutorial.* <https://search.maven.org/search?q=g:org.kurento.tutorial>`__)
* ``test/tutorial``

JavaScript follows a similar ordering. Starting from :ref:`dev-release-javascript` for the main Kurento modules:

* ``server/module-core`` (`kurento-client-core <https://www.npmjs.com/package/kurento-client-core>`__)
* ``server/module-elements`` (`kurento-client-elements <https://www.npmjs.com/package/kurento-client-elements>`__)
* ``server/module-filters`` (`kurento-client-filters <https://www.npmjs.com/package/kurento-client-filters>`__)

* ``clients/javascript/jsonrpc`` (`kurento-jsonrpc <https://www.npmjs.com/package/kurento-jsonrpc>`__)
* ``clients/javascript/client`` (`kurento-client <https://www.npmjs.com/package/kurento-client>`__)

Example Kurento modules:

* ``server/module-examples/chroma`` (`kurento-module-chroma <https://www.npmjs.com/package/kurento-module-chroma>`__)
* ``server/module-examples/crowddetector`` (`kurento-module-crowddetector <https://www.npmjs.com/package/kurento-module-crowddetector>`__)
* ``server/module-examples/datachannelexample`` (`kurento-module-datachannelexample <https://www.npmjs.com/package/kurento-module-datachannelexample>`__)
* ``server/module-examples/markerdetector`` (`kurento-module-markerdetector <https://www.npmjs.com/package/kurento-module-markerdetector>`__)
* ``server/module-examples/platedetector`` (`kurento-module-platedetector <https://www.npmjs.com/package/kurento-module-platedetector>`__)
* ``server/module-examples/pointerdetector`` (`kurento-module-pointerdetector <https://www.npmjs.com/package/kurento-module-pointerdetector>`__)

And tutorials:

* ``tutorials/javascript-node``
* ``tutorials/javascript-browser``

Last, but not least, the project maintains a set of Docker images and documentation pages:

* :ref:`dev-release-docker`
* :ref:`dev-release-doc`



FIRST: Open a new Release Process
=================================

To start with a new release, first of all create a new git branch that will contain all changes related to the release itself:

.. code-block:: shell

   git switch --create release-1.0.0
   git push --set-upstream origin HEAD



.. _dev-release-media-server:

Kurento Media Server
====================

All KMS projects:

.. graphviz:: /images/graphs/dependencies-media-server.dot
   :align: center
   :caption: Projects that are part of Kurento Media Server

Release order:

* ``server/module-creator``
* ``server/cmake-utils``
* ``server/jsonrpc``
* ``server/module-core``
* ``server/module-elements``
* ``server/module-filters``
* ``server/media-server``

* ``server/module-examples/chroma``
* ``server/module-examples/crowddetector``
* ``server/module-examples/datachannelexample``
* ``server/module-examples/markerdetector``
* ``server/module-examples/platedetector``
* ``server/module-examples/pointerdetector``



Preparation: kurento-module-creator
-----------------------------------

* If *kurento-maven-plugin* is getting a new version, edit the file ``server/module-creator/src/main/templates/maven/model_pom_xml.ftl`` to update it:

  .. code-block:: diff

        <groupId>org.kurento</groupId>
        <artifactId>kurento-maven-plugin</artifactId>
     -  <version>1.0.0</version>
     +  <version>1.1.0</version>

Build the new version (if any), install it to the Maven cache, and set the ``PATH`` appropriately:

.. code-block:: shell

   cd server/module-creator/
   mvn -DskipTests=false clean install
   export PATH="$PWD/scripts:$PATH"



Preparation: kurento-maven-plugin
---------------------------------

Build the new version (if any) and install it to the Maven cache:

.. code-block:: shell

   cd clients/java/maven-plugin/
   mvn -DskipTests=false clean install



Preparation: API modules
------------------------

**Local check**: Test that the KMS API module generation works.

Note that if the generation templates (``*.ftl``) have been changed, you'll probably need them to be in effect, and for that you'll need to use a local build of the Kurento Module Creator, instead of using the version that gets installed with the *kurento-module-creator* package.

This is the command to generate and build a Java module:

.. code-block:: shell

   mkdir build/ && cd build/ \
      && cmake -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE .. \
      && cd java/ \
      && mvn -DskipTests=true clean install

For JavaScript modules, the command is very similar:

.. code-block:: shell

   mkdir build/ && cd build/ \
      && cmake -DGENERATE_JS_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE .. \
      && cd js/ \
      && npm install

Complete code for Java and JavaScript modules:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       kurento-module-creator \
       kurento-cmake-utils \
       kurento-jsonrpc-dev \
       kurento-module-core-dev \
       kurento-module-elements-dev \
       kurento-module-filters-dev

   cd server/

   function do_release {
       local PROJECTS=(
           module-core
           module-elements
           module-filters
           module-examples/chroma
           #module-examples/crowddetector
           module-examples/datachannelexample
           #module-examples/markerdetector
           #module-examples/platedetector
           #module-examples/pointerdetector
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           mkdir -p build/ && cd build/

           cmake -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE .. \
               && pushd java/ \
               && mvn -DskipTests=true clean install \
               && popd \
               || { echo "ERROR: Java code generation failed"; return 1; }

           cmake -DGENERATE_JS_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE .. \
               && pushd js/ \
               && npm install \
               && popd \
               || { echo "ERROR: JavaScript code generation failed"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   ( set -o xtrace; do_release; )



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Set the new version. Most modules have a ``bin/set-version.sh`` script to make it easier with per-project specific commands.

#. Check there are no dangling development versions in any of the dependencies.

   Search for the ``-dev`` suffix.

#. Commit (rebase and squash as needed) and push changes.

#. Run the `Server Build All`_ job with parameters:

   - *jobGitName*: Release branch name (e.g. *release-1.0.0*).
   - *jobRelease*: **ENABLED**.
   - *jobOnlyKurento*: **DISABLED**.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0
   NEW_DEBIAN="<DebianRevision>"  # Eg.: 1kurento1

   cd server/

   function do_release {
       # Set the new version.
       bin/set-versions.sh "$NEW_VERSION" --debian "$NEW_DEBIAN" \
           --release --commit \
       || { echo "ERROR: Command failed: set-versions"; return 1; }

       # Check for development versions.
       grep -Pr \
           --include CMakeLists.txt \
           --include '*.cmake' \
           --include '*.kmd.json' \
           --exclude-dir 'test*/' \
           -- '\d+\.\d+\.\d+-dev' \
       && { echo "ERROR: Development versions not allowed!"; return 1; }

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   ( set -o xtrace; do_release; )

   # Review committed changes. Amend as needed.
   git log --max-count 1 --patch

   # Push committed changes.
   git push



.. _dev-release-javascript:

Kurento JavaScript client
=========================

Release order:

* ``browser/kurento-utils-js`` (`kurento-utils <https://www.npmjs.com/package/kurento-utils>`__)

* ``server/module-core`` (`kurento-client-core <https://www.npmjs.com/package/kurento-client-core>`__)
* ``server/module-elements`` (`kurento-client-elements <https://www.npmjs.com/package/kurento-client-elements>`__)
* ``server/module-filters`` (`kurento-client-filters <https://www.npmjs.com/package/kurento-client-filters>`__)

* ``clients/javascript/jsonrpc`` (`kurento-jsonrpc <https://www.npmjs.com/package/kurento-jsonrpc>`__)
* ``clients/javascript/client`` (`kurento-client <https://www.npmjs.com/package/kurento-client>`__)

Example Kurento modules:

* ``server/module-examples/chroma`` (`kurento-module-chroma <https://www.npmjs.com/package/kurento-module-chroma>`__)
* ``server/module-examples/crowddetector`` (`kurento-module-crowddetector <https://www.npmjs.com/package/kurento-module-crowddetector>`__)
* ``server/module-examples/datachannelexample`` (`kurento-module-datachannelexample <https://www.npmjs.com/package/kurento-module-datachannelexample>`__)
* ``server/module-examples/markerdetector`` (`kurento-module-markerdetector <https://www.npmjs.com/package/kurento-module-markerdetector>`__)
* ``server/module-examples/platedetector`` (`kurento-module-platedetector <https://www.npmjs.com/package/kurento-module-platedetector>`__)
* ``server/module-examples/pointerdetector`` (`kurento-module-pointerdetector <https://www.npmjs.com/package/kurento-module-pointerdetector>`__)

And tutorials:

* ``tutorials/javascript-node``
* ``tutorials/javascript-browser``



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Set the new version. Most modules have a ``bin/set-version.sh`` script to make it easier with per-project specific commands.

#. Check there are no dangling development versions in any of the dependencies.

   Search for the ``-dev`` suffix.

#. Commit (rebase and squash as needed) and push changes.

#. Run the `Clients Build All JavaScript`_ job with parameters:

   - *jobRelease*: **ENABLED**.
   - *jobServerVersion*: Repository name of the release branch name (e.g. *dev-release-1.0.0*).

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0

   function do_release {
       local PROJECTS=(
           browser/kurento-utils-js
           clients/javascript
           tutorials/javascript-node
           tutorials/javascript-browser
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" \
           || { echo "ERROR: Command failed: pushd"; return 1; }

           # Set the new version.
           bin/set-versions.sh "$NEW_VERSION" --release --commit \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           # Check for development versions.
           grep -Pr --exclude-dir node_modules --include package.json -- '-dev|git\+http' \
           && { echo "ERROR: Development versions not allowed!"; return 1; }

           # Test the build.
           if [[ "$PROJECT" == "clients/javascript" ]]; then
               # kurento-client depends on kurento-jsonrpc, so install it
               # directly here to resolve the dependency.
               # Do not use `npm link`, because it is broken [1] and the link
               # will be lost with the `npm install` that comes afterwards.
               # [1]: https://github.com/npm/cli/issues/2372
               pushd jsonrpc/ && npm install && popd
               cd client/
               npm install \
                   ../jsonrpc/ \
                   ../../../server/module-core/build/js/ \
                   ../../../server/module-elements/build/js/ \
                   ../../../server/module-filters/build/js/
           fi
           if [[ -f package.json ]]; then
               npm install || { echo "ERROR: Command failed: npm install"; return 1; }
           fi
           if [[ -x node_modules/.bin/grunt ]]; then
               node_modules/.bin/grunt jsbeautifier \
               && node_modules/.bin/grunt \
               && node_modules/.bin/grunt sync:bower \
               || { echo "ERROR: Command failed: grunt"; return 1; }
           fi

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   ( set -o xtrace; do_release; )



Post-Release
------------

If CI jobs fail, the most common issue is that the code is not properly formatted. To manually run the beautifier, do this:

.. code-block:: shell

   npm install

   # To run beautifier over all files, modifying in-place:
   node_modules/.bin/grunt jsbeautifier::default

   # To run beautifier over a specific file:
   node_modules/.bin/grunt jsbeautifier::file:<FilePath>.js

When all CI jobs have finished successfully:

* Check that the auto-generated JavaScript client repos have been updated with the new version:

  - `kurento-client-core-js <https://github.com/Kurento/kurento-client-core-js>`__
  - `kurento-client-elements-js <https://github.com/Kurento/kurento-client-elements-js>`__
  - `kurento-client-filters-js <https://github.com/Kurento/kurento-client-filters-js>`__

  - `kurento-module-chroma-js <https://github.com/Kurento/kurento-module-chroma-js>`__
  - `kurento-module-crowddetector-js <https://github.com/Kurento/kurento-module-crowddetector-js>`__
  - `kurento-module-datachannelexample-js <https://github.com/Kurento/kurento-module-datachannelexample-js>`__
  - `kurento-module-markerdetector-js <https://github.com/Kurento/kurento-module-markerdetector-js>`__
  - `kurento-module-platedetector-js <https://github.com/Kurento/kurento-module-platedetector-js>`__
  - `kurento-module-pointerdetector-js <https://github.com/Kurento/kurento-module-pointerdetector-js>`__

* Check that the JavaScript packages have been published to NPM:

  - NPM: `kurento-client-core <https://www.npmjs.com/package/kurento-client-core>`__
  - NPM: `kurento-client-elements <https://www.npmjs.com/package/kurento-client-elements>`__
  - NPM: `kurento-client-filters <https://www.npmjs.com/package/kurento-client-filters>`__

* Open the `Nexus Sonatype Staging Repositories`_ section.
* Select **kurento** repository.
* Inspect **Content** to ensure they are as expected:

  - kurento-module-chroma-js
  - kurento-module-crowddetector-js
  - kurento-module-datachannelexample-js
  - kurento-module-markerdetector-js
  - kurento-module-platedetector-js
  - kurento-module-pointerdetector-js

  - kurento-utils-js
  - kurento-jsonrpc-js
  - kurento-client-js

  All of them must appear in the correct version, ``$NEW_VERSION``.

* **Close** repository.
* Wait a bit.
* **Refresh**.
* **Release** repository.
* Maven artifacts will be available `within 30 minutes <https://central.sonatype.org/publish/publish-guide/#releasing-to-central>`__.



.. _dev-release-java:

Kurento Java client
===================

Release order:

* ``server/module-creator`` (`org.kurento.kurento-module-creator <https://search.maven.org/artifact/org.kurento/kurento-module-creator>`__)
* ``clients/java/maven-plugin`` (`org.kurento.kurento-maven-plugin <https://search.maven.org/artifact/org.kurento/kurento-maven-plugin>`__)
* ``clients/java/qa-pom`` (`org.kurento.kurento-qa-pom <https://search.maven.org/artifact/org.kurento/kurento-qa-pom>`__)

* ``server/module-core`` (`org.kurento.kms-api-core <https://search.maven.org/artifact/org.kurento/kms-api-core>`__)
* ``server/module-elements`` (`org.kurento.kms-api-elements <https://search.maven.org/artifact/org.kurento/kms-api-elements>`__)
* ``server/module-filters`` (`org.kurento.kms-api-filters <https://search.maven.org/artifact/org.kurento/kms-api-filters>`__)

* ``clients/java`` (`org.kurento.kurento-java <https://search.maven.org/artifact/org.kurento/kurento-java>`__, including `org.kurento.kurento-client <https://search.maven.org/artifact/org.kurento/kurento-client>`__)

After *kurento-client* is done, the client code for example Kurento modules can be built:

* ``server/module-examples/chroma`` (`org.kurento.module.chroma <https://search.maven.org/artifact/org.kurento.module/chroma>`__)
* ``server/module-examples/crowddetector`` (`org.kurento.module.crowddetector <https://search.maven.org/artifact/org.kurento.module/crowddetector>`__)
* ``server/module-examples/datachannelexample`` (`org.kurento.module.datachannelexample <https://search.maven.org/artifact/org.kurento.module/datachannelexample>`__)
* ``server/module-examples/markerdetector`` (`org.kurento.module.markerdetector <https://search.maven.org/artifact/org.kurento.module/markerdetector>`__)
* ``server/module-examples/platedetector`` (`org.kurento.module.platedetector <https://search.maven.org/artifact/org.kurento.module/platedetector>`__)
* ``server/module-examples/pointerdetector`` (`org.kurento.module.pointerdetector <https://search.maven.org/artifact/org.kurento.module/pointerdetector>`__)

Now, the Kurento testing packages (which depend on some of the example modules). *kurento-utils-js* library must also be built at this stage, because it is a dependency of *kurento-test*:

* ``browser/kurento-utils-js`` (`kurento-utils <https://www.npmjs.com/package/kurento-utils>`__)
* ``test/integration`` (`org.kurento.kurento-integration-tests <https://search.maven.org/artifact/org.kurento/kurento-integration-tests>`__, including `org.kurento.kurento-test <https://search.maven.org/artifact/org.kurento/kurento-test>`__)

And lastly, the tutorials (which depend on the example modules):

* ``tutorials/java`` (`org.kurento.tutorial.kurento-tutorial <https://search.maven.org/artifact/org.kurento.tutorial/kurento-tutorial>`__, including `org.kurento.tutorial.* <https://search.maven.org/search?q=g:org.kurento.tutorial>`__)
* ``test/tutorial``

Dependency graph:

.. graphviz:: /images/graphs/dependencies-java.dot
   :align: center
   :caption: Java dependency graph



Preparation: kurento-java
-------------------------

* If *kurento-maven-plugin* is getting a new version, edit the file ``clients/java/parent-pom/pom.xml`` to update it:

  .. code-block:: diff

     -  <version.kurento-maven-plugin>1.0.0</version.kurento-maven-plugin>
     +  <version.kurento-maven-plugin>1.1.0</version.kurento-maven-plugin>


* If *kurento-utils-js* is getting a new version, edit the file ``clients/java/parent-pom/pom.xml`` to update it:

  .. code-block:: diff

     -  <version.kurento-utils-js>1.0.0</version.kurento-utils-js>
     +  <version.kurento-utils-js>1.1.0</version.kurento-utils-js>



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Set the new version. Most modules have a ``bin/set-version.sh`` script to make it easier with per-project specific commands.

#. Check there are no dangling development versions in any of the dependencies.

   Search for the ``-SNAPSHOT`` suffix. Note that most versions are defined as properties in ``clients/java/parent-pom/pom.xml``.

#. Commit (rebase and squash as needed) and push changes.

#. Run the `Clients Build All Java`_ job with parameters:

   - *jobServerVersion*: Repository name of the release branch name (e.g. *dev-release-1.0.0*).

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0

   function do_release {
       local PROJECTS=(
           clients/java/qa-pom
           clients/java
           tutorials/java
           test/integration
           test/tutorial
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" \
           || { echo "ERROR: Command failed: pushd"; return 1; }

           # Set the new version.
           bin/set-versions.sh "$NEW_VERSION" --kms-api "$NEW_VERSION" \
               --release --commit \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           # Check for development versions.
           grep -Fr --include pom.xml -- '-SNAPSHOT' \
           && { echo "ERROR: Development versions not allowed!"; return 1; }

           # Skip "test/tutorial" because the testing framework has rotted
           # and isn't able to spawn new browser windows for the tests.
           if [[ "$PROJECT" == "test/tutorial" ]]; then
               continue
           fi

           # Install the project.
           # * Build and run tests.
           # * Do not use `-U` because for each project we want Maven to find
           #   the locally cached artifacts from previous project.
           mvn -Pkurento-release -DskipTests=false clean install \
           || { echo "ERROR: Command failed: mvn"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   ( set -o xtrace; do_release; )



Post-Release
------------

When all CI jobs have finished successfully:

* Open the `Nexus Sonatype Staging Repositories`_ section.
* Select **kurento** repository.
* Inspect **Content** to ensure it is as expected:

  - org.kurento.kms-api-core
  - org.kurento.kms-api-elements
  - org.kurento.kms-api-filters
  - org.kurento.kurento-client
  - org.kurento.kurento-commons
  - org.kurento.kurento-java
  - org.kurento.kurento-jsonrpc
  - org.kurento.kurento-jsonrpc-client
  - org.kurento.kurento-jsonrpc-client-jetty
  - org.kurento.kurento-jsonrpc-server
  - org.kurento.kurento-maven-plugin
  - org.kurento.kurento-module-creator
  - org.kurento.kurento-parent-pom
  - org.kurento.kurento-qa-config
  - org.kurento.kurento-qa-pom
  - org.kurento.module.chroma
  - org.kurento.module.crowddetector (Unavailable since Kurento 7.0.0)
  - org.kurento.module.datachannelexample
  - org.kurento.module.markerdetector (Unavailable since Kurento 7.0.0)
  - org.kurento.module.platedetector (Unavailable since Kurento 7.0.0)
  - org.kurento.module.pointerdetector (Unavailable since Kurento 7.0.0)

  All of them must appear in the correct version, ``$NEW_VERSION``.

* **Close** repository.
* Wait a bit.
* **Refresh**.
* **Release** repository.
* Maven artifacts will be available `within 30 minutes <https://central.sonatype.org/publish/publish-guide/#releasing-to-central>`__.



.. _dev-release-docker:

Docker images
=============

A new set of development images is deployed to `Kurento Docker Hub`_ on each build. Besides, a release version will be published as part of the CI jobs chain when the `Server Build All`_ job is triggered.



.. _dev-release-doc:

Kurento documentation
=====================

The documentation scripts will download both Java and JavaScript clients, generate HTML Javadoc / Jsdoc pages from them, and embed everything into a `static section <https://doc-kurento.readthedocs.io/en/latest/features/kurento_client.html#reference-documentation>`__.

For this reason, the documentation must be built only after all the other modules have been released.

#. Write the Release Notes in ``doc-kurento/source/project/relnotes/``.

#. Edit ``doc-kurento/VERSIONS.env`` to set all relevant version numbers: version of the documentation itself, and all referred modules and client libraries.

   These numbers can be different because not all of the Kurento projects are necessarily released with the same frequency. Check each one of the Kurento modules to verify what is the latest version of each one, and put it in the corresponding variable:

   - ``[VERSION_DOC]``: The docs version shown to readers. Normally same as ``[VERSION_KMS]``.
   - ``[VERSION_KMS]``: Version of the Kurento Media Server
   - ``[VERSION_CLIENT_JAVA]``: Version of the Java client SDK
   - ``[VERSION_CLIENT_JS]``: Version of the JavaScript client SDK
   - ``[VERSION_UTILS_JS]``: Version of *kurento-utils-js*
   - ``[VERSION_TUTORIAL_JAVA]``: Version of the Java tutorials package.
   - ``[VERSION_TUTORIAL_NODE]``: Version of the Node.js tutorials package.
   - ``[VERSION_TUTORIAL_JS]``: Version of the Browser JavaScript tutorials package.

#. In ``doc-kurento/VERSIONS.env``, set *VERSION_RELEASE* to **true**. Remember to set it again to *false* after the release, when starting a new development iteration.

#. Test the build locally, check everything works.

   .. code-block:: shell

      python3 -m venv python_modules
      source python_modules/bin/activate
      python3 -m pip install --upgrade -r requirements.txt
      make html

   Repeat the *pip install* command if it fails. Python package management is so abysmally bad that this usually solves the issue.

   JavaDoc and JsDoc pages can be generated separately with ``make langdoc``.

#. Commit (rebase and squash as needed) and push changes.

#. Run the `Documentation build`_ job with parameters:

   - Workflow branch: Release branch name (e.g. *release-1.0.0*).
   - *jobRelease*: **ENABLED**.

#. CI automatically tags Release versions in the ReadTheDocs-generated repo `doc-kurento-readthedocs`_, so the release will show up in the ReadTheDocs dashboard.

   .. note::

      If you made a mistake and want to re-create the git tag with a different commit, remember that the re-tagging must be done manually in the *doc-kurento-readthedocs* repo. ReadTheDocs reads it to determine the documentation sources and release tags.

#. Check for errors in `ReadTheDocs Builds`_. If the new version hasn't been detected and built, do it manually: use the *Build Version* button to force a build of the *latest* version. Doing this, ReadTheDocs will detect that there is a new tag in the *doc-kurento-readthedocs* repo.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0

   cd doc-kurento/

   function do_release {
       local COMMIT_MSG="Prepare documentation release $NEW_VERSION"
       local SHORT_VERSION="${NEW_VERSION%.*}" # Major.Minor (no .Patch)

       # Set [VERSION_RELEASE]="true".
       sed -r -i 's/\[VERSION_RELEASE\]=.*/[VERSION_RELEASE]="true"/' VERSIONS.env \
       || { echo "ERROR: Command failed: sed"; return 1; }

       # Set [VERSION_DOC].
       local VERSION_DOC="$SHORT_VERSION"
       sed -r -i "s/\[VERSION_DOC\]=.*/[VERSION_DOC]=\"$VERSION_DOC\"/" VERSIONS.env \
       || { echo "ERROR: Command failed: sed"; return 2; }

       # `--all` to include possibly deleted files.
       git add --all \
           VERSIONS.env \
           source/project/relnotes/ \
       && git commit -m "$COMMIT_MSG" \
       || { echo "ERROR: Command failed: git"; return 4; }

       echo "Done!"
   }

   # Run in a subshell where all commands are traced
   ( set -o xtrace; do_release; )



LAST: Close the Release Process
===============================

To finish the release, go to GitHub and create a new Pull Request from the release branch:

.. code-block:: text

   https://github.com/Kurento/kurento/pull/new/release-1.0.0

Review all the changes, and accept the PR with a **merge commit**. Do not use the other options (squash or rebase), because we want all changes to get separately recorded with author and date information.

After merging the PR, fetch the new commit:

.. code-block:: shell

   git switch main
   git fetch --all --tags --prune --prune-tags
   git pull --autostash

Tag the commit with the new version number (change ``1.0.0`` to the correct one):

.. code-block:: shell

   git tag --force --annotate -m "1.0.0" "1.0.0"
   git push --force origin "1.0.0"

And now you can optionally do some cleanup in your local clone:

.. code-block:: shell

   git branch --force --delete release-1.0.0

Then, proceed to start a new development iteration for all of the Kurento components.



Kurento Media Server
--------------------

#. Bump to a new development version. Do this by incrementing the *.PATCH* number and resetting the **Debian revision** to 1.

#. Run the `Server Build All`_ job with parameters:

   - *jobGitName*: (Default value).
   - *jobRelease*: **DISABLED**.
   - *jobOnlyKurento*: **DISABLED**.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>"   # Eg.: 1.0.1
   NEW_DEBIAN="<DebianRevision>" # Eg.: 1kurento1

   cd server/

   # Set the new version.
   bin/set-versions.sh "$NEW_VERSION" --debian "$NEW_DEBIAN" \
       --new-development --commit



Kurento JavaScript client
-------------------------

#. Bump to a new development version. Do this by incrementing the *.PATCH* number.

#. Run the `Clients Build All JavaScript`_ job with parameters:

   - *jobRelease*: **DISABLED**.
   - *jobServerVersion*: (Default value).

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>" # Eg.: 1.0.1

   function do_release {
       local PROJECTS=(
           browser/kurento-utils-js
           clients/javascript
           tutorials/javascript-node
           tutorials/javascript-browser
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" \
           || { echo "ERROR: Command failed: pushd"; return 1; }

           # Set the new version.
           bin/set-versions.sh "$NEW_VERSION" --new-development --commit \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   ( set -o xtrace; do_release; )



Kurento Java client
-------------------

#. Bump to a new development version. Do this by incrementing the *.PATCH* number.

#. Run the `Clients Build All Java`_ job with parameters:

   - *jobServerVersion*: (Default value).

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>" # Eg.: 1.0.1

   function do_release {
       local PROJECTS=(
           clients/java/qa-pom
           clients/java
           tutorials/java
           test/integration
           test/tutorial
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" \
           || { echo "ERROR: Command failed: pushd"; return 1; }

           # Set the new version.
           bin/set-versions.sh "$NEW_VERSION" --kms-api "$NEW_VERSION-SNAPSHOT" \
               --new-development --commit \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   ( set -o xtrace; do_release; )



Kurento documentation
---------------------

#. Set *VERSION_RELEASE* to **false**.

#. Create a Release Notes document template where to write changes that will accumulate for the next release.

#. Run the `Documentation build`_ job with parameters:

   - *jobRelease*: **DISABLED**.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>" # Eg.: 1.0.1
   IS_MAJOR="<IsMajor?>" # "true" for 1.1.0, "false" for 1.0.1

   cd doc-kurento/

   function do_release {
       local SHORT_VERSION="${NEW_VERSION%.*}" # Major.Minor (no .Patch)

       # Set [VERSION_RELEASE]="false"
       sed -r -i 's/\[VERSION_RELEASE\]=.*/[VERSION_RELEASE]="false"/' VERSIONS.env \
       || { echo "ERROR: Command failed: sed"; return 1; }

       # Set [VERSION_DOC]
       local VERSION_DOC="$SHORT_VERSION-dev"
       sed -r -i "s/\[VERSION_DOC\]=.*/[VERSION_DOC]=\"$VERSION_DOC\"/" VERSIONS.env \
       || { echo "ERROR: Command failed: sed"; return 2; }

       # Add a new Release Notes document
       if [[ "$IS_MAJOR" == "true" ]]; then
           local RELNOTES_NAME="$SHORT_VERSION"
           cp source/project/relnotes/0.0_TEMPLATE.rst \
               "source/project/relnotes/$RELNOTES_NAME.rst" \
           && sed -i "s/00.00/$RELNOTES_NAME/" \
               "source/project/relnotes/$RELNOTES_NAME.rst" \
           && sed -i "8i\   $RELNOTES_NAME" \
               source/project/relnotes/index.rst \
           || { echo "ERROR: Command failed: sed"; return 3; }
       fi

       # Amend last commit with these changes.
       # This assumes that previous modules have been committed already,
       # otherwise just replace `--amend --no-edit`
       # with `-m "Prepare for next development iteration"`.
       git add \
           VERSIONS.env \
           source/project/relnotes/ \
       && git commit --amend --no-edit \
       || { echo "ERROR: Command failed: git"; return 4; }

       echo "Done!"
   }

   # Run in a subshell where all commands are traced
   ( set -o xtrace; do_release; )



.. Kurento links
.. _Kurento Docker Hub: https://hub.docker.com/u/kurento
.. _Kurento Docker images: https://hub.docker.com/r/kurento/kurento-media-server
.. _Server Build All: https://github.com/Kurento/kurento/actions/workflows/server-parent.yaml
.. _Clients Build All Java: https://github.com/Kurento/kurento/actions/workflows/clients-java-parent.yaml
.. _Clients Build All JavaScript: https://github.com/Kurento/kurento/actions/workflows/clients-javascript-parent.yaml
.. _Documentation build: https://ci.openvidu.io/jenkins/job/Development/job/kurento_doc_merged/
.. _doc-kurento-readthedocs: https://github.com/Kurento/doc-kurento-readthedocs



.. External links
.. _Debian Policy Manual: https://www.debian.org/doc/debian-policy/ch-controlfields.html#version
.. _this Ask Ubuntu answer: https://askubuntu.com/questions/620533/what-is-the-meaning-of-the-xubuntuy-string-in-ubuntu-package-names/620539#620539
.. _Semantic Versioning: https://semver.org/spec/v2.0.0.html#summary
.. _Aptly: https://www.aptly.info/
.. _Nexus Sonatype Staging Repositories: https://oss.sonatype.org/#stagingRepositories
.. _ReadTheDocs Builds: https://readthedocs.org/projects/doc-kurento/builds/
.. _New build at ReadTheDocs: https://readthedocs.org/projects/doc-kurento/builds/
.. _ReadTheDocs Advanced Settings: https://readthedocs.org/dashboard/doc-kurento/advanced/
