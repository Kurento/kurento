==================
Release Procedures
==================

.. contents:: Table of Contents



Introduction
============

Kurento as a project spans across a multitude of different technologies and languages, each of them with their sets of conventions and *best practices*. This document aims to summarize all release procedures that apply to each one of the modules that compose the Kurento project. The main form of categorization is by technology type: C/C++ based modules, Java modules, JavaScript modules, and others.


.. _dev-release-general:

General considerations
======================

* Lists of projects in this document are sorted according to the repository lists given in :ref:`dev-code-repos`.

* Kurento projects to be released have supposedly been under development, and will have development version numbers:

  - In Java (Maven) projects, development versions are indicated by the suffix ``-SNAPSHOT`` after the version number. Example: ``1.0.0-SNAPSHOT``.
  - In C/C++ (CMake) projects, development versions are indicated by the suffix ``-dev`` after the version number. Example: ``1.0.0-dev``.

  These suffixes must be removed for release, and then recovered again to resume development.

* All dependencies to development versions will be changed to a release version during the release procedure. Concerning people will be asked to choose an appropriate release version for each development dependency.

* Tags are named with the version number of the release. Example: ``1.0.0``.

* Contrary to the project version, the Debian package versions don't contain development suffixes, and should always be of the form ``1.0.0-0kurento1``:

  - The first part (*1.0.0*) is the project's **base version number**.

  - The second part (*0kurento1*) is the **Debian package revision**:

    - The number prefix (in this example: *0*) indicates the version relative to other same-name packages provided by the base system. When this number is *0*, it means that the package is original and only exists in Kurento, not in Debian or Ubuntu itself. This is typically the case for the projects owned or forked for the Kurento project.

    - The number suffix (in this example: *1*) means the number of times the same package has been re-packaged and re-published. *1* means that this is the *first* time a given project version was packaged.

    **Example**:

    Imagine that version *1.0.0* of your code is released for the first time. The full Debian package version will be: *1.0.0-0kurento1*. Later, you realize the package doesn't install correctly in some machines, because of a bug in the package's post-install script. You fix it, and now it's time to re-publish! But the project's source code itself has not changed at all, only the packaging files (in ``/debian/`` dir); thus, the *base version* will remain *1.0.0*, and only the *Debian revision* needs to change. The new package's full version will be *1.0.0-0kurento2*.

  Please check the `Debian Policy Manual`_ and `this Ask Ubuntu answer`_ for more information about the package versions.

* Kurento uses `Semantic Versioning`_. Whenever you need to decide what is going to be the *final release version* for a new release, try to follow the SemVer guidelines:

  .. code-block:: text

     Given a version number MAJOR.MINOR.PATCH, increment the:

     1. MAJOR version when you make incompatible API changes,
     2. MINOR version when you add functionality in a backwards-compatible manner, and
     3. PATCH version when you make backwards-compatible bug fixes.

  Please refer to https://semver.org/ for more information.

  **Example**:

  If the last Kurento release was **1.0.0**, then the next development version would be **1.0.1-dev** (or *1.0.1-SNAPSHOT* for Java components).

  Later, the time comes to release this new development. If the new code only includes bug fixes and patches, then the version number *1.0.1* is already good. However, maybe this new release ended up including new functionality, which according to Semantic Versioning should be accompanied with a bump in the *.MINOR* version number, so the next release version number should be *1.1.0*. The Debian package version is reset accordingly, so the full Debian version is **1.1.0-0kurento1**.

  If you are re-packaging an already published version, without changes in the project's code itself, then just increment the Debian package revision: *0kurento1* becomes *0kurento2*, and so on.



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



.. warning::

   As of this writing, there is a mix of methods in the CI scripts (adm-scripts) when it comes to handle the release versions. The instructions in this document favor creating and pushing git tags manually in the developer's computer, however some projects also make use of the script *kurento_check_version.sh*, which tries to detect when a project's version is *not* a development snapshot, then creates and pushes a git tag automatically. However if the tag already exists (created manually by the developer), then the ``git tag`` command fails, and this script prints a warning message before continuing with its work.

   We've been toying with different methodologies between handling the tags automatically in CI or handling them manually by the developer before releasing new versions; both of these methods have pros and cons. For example, if tags are handled manually by the developer, solving mistakes in the release process becomes simpler because there are no surprises from CI creating tags inadvertently; on the other hand, leaving them to be created by CI seems to simplify a bit the release process, but not really by a big margin.



Fork Repositories
=================

This graph shows the dependencies between forked projects used by Kurento:

.. graphviz:: /images/graphs/dependencies-forks.dot
   :align: center
   :caption: Projects forked by Kurento

Release order:

* `jsoncpp`_
* `libsrtp`_
* `openh264`_
* `openh264-gst-plugin`_
* `libusrsctp`_
* `gstreamer`_
* `gst-plugins-base`_
* `gst-plugins-good`_
* `gst-plugins-bad`_
* `gst-plugins-ugly`_
* `gst-libav`_
* `openwebrtc-gst-plugins`_
* `libnice`_



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Set the new version. This operation might vary between projects.

   .. code-block:: shell

      # Change here.
      NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0
      NEW_DEBIAN="<DebianRevision>"  # Eg.: 0kurento1

      function do_release {
          local PACKAGE_VERSION="${NEW_VERSION}-${NEW_DEBIAN}"
          local COMMIT_MSG="Prepare release $PACKAGE_VERSION"

          local SNAPSHOT_ENTRY="* UNRELEASED"
          local RELEASE_ENTRY="* $COMMIT_MSG"

          gbp dch \
              --ignore-branch \
              --git-author \
              --spawn-editor never \
              --new-version "$PACKAGE_VERSION" \
              \
              --release \
              --distribution "testing" \
              --force-distribution \
              \
              debian/ \
          || { echo "ERROR: Command failed: gbp dch"; return 1; }

          # First appearance of "UNRELEASED": Put our commit message
          sed -i "0,/${SNAPSHOT_ENTRY}/{s/${SNAPSHOT_ENTRY}/${RELEASE_ENTRY}/}" \
              debian/changelog \
          || { echo "ERROR: Command failed: sed"; return 2; }

          # Remaining appearances of "UNRELEASED" (if any): Delete line
          sed -i "/${SNAPSHOT_ENTRY}/d" \
              debian/changelog \
          || { echo "ERROR: Command failed: sed"; return 3; }

          git add debian/changelog \
          && git commit -m "$COMMIT_MSG" \
          || { echo "ERROR: Command failed: git"; return 4; }

          gbp tag \
          && gbp push \
          || { echo "ERROR: Command failed: gbp"; return 5; }

          echo "Done!"
      }

      # Run in a subshell where all commands are traced.
      (set -o xtrace; do_release)

#. Follow on with releasing Kurento Media Server.



New Development
---------------

**After the whole release has been completed**, bump to a new development version. Do this by incrementing the *Debian revision* number.

The version number (as opposed to the Debian revision) is only changed when the fork gets updated from upstream sources. Meanwhile, we only update the Debian revision.

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>"   # Eg.: 1.0.1
   NEW_DEBIAN="<DebianRevision>" # Eg.: 0kurento1

   function do_release {
       local PACKAGE_VERSION="${NEW_VERSION}-${NEW_DEBIAN}"
       local COMMIT_MSG="Bump development version to $PACKAGE_VERSION"

       gbp dch \
             --ignore-branch \
             --git-author \
             --spawn-editor never \
             --new-version "$PACKAGE_VERSION" \
             debian/ \
       || { echo "ERROR: Command failed: gbp dch"; return 1; }

       git add debian/changelog \
       && git commit -m "$COMMIT_MSG" \
       || { echo "ERROR: Command failed: git"; return 2; }

       gbp tag \
       && gbp push \
       || { echo "ERROR: Command failed: gbp"; return 3; }

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   (set -o xtrace; do_release)



Kurento Media Server
====================

All KMS projects:

.. graphviz:: /images/graphs/dependencies-kms.dot
   :align: center
   :caption: Projects that are part of Kurento Media Server

Release order:

* `kurento-module-creator`_
* `kurento-maven-plugin`_
* `kms-cmake-utils`_
* `kms-jsonrpc`_
* `kms-core`_
* `kms-elements`_
* `kms-filters`_
* `kurento-media-server`_

* `kms-chroma`_
* `kms-crowddetector`_
* `kms-datachannelexample`_
* `kms-markerdetector`_
* `kms-platedetector`_
* `kms-pointerdetector`_



Preparation: Kurento Module Creator
-----------------------------------

If *kurento-maven-plugin* is going to get also a new release, then edit the file ``kurento-module-creator/src/main/templates/maven/model_pom_xml.ftl`` to update the plugin version in the auto-generation template:

.. code-block:: xml

      <groupId>org.kurento</groupId>
      <artifactId>kurento-maven-plugin</artifactId>
   -  <version>1.0.0</version>
   +  <version>1.1.0</version>

Then proceed with the normal release.



Preparation: KMS API Java modules
---------------------------------

Test the KMS API Java module generation (local check).

.. code-block:: shell

   apt-get update && apt-get install --no-install-recommends --yes \
       kurento-module-creator \
       kms-cmake-utils \
       kms-jsonrpc-dev \
       kms-core-dev \
       kms-elements-dev \
       kms-filters-dev

   cd kms-omni-build

   function do_release {
       local PROJECTS=(
           kms-core
           kms-elements
           kms-filters
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           mkdir build \
           && cd build \
           && cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE \
           && cd java \
           && mvn clean install -Dmaven.test.skip=false \
           || { echo "ERROR: Command failed"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   (set -o xtrace; do_release)



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Set the new version. This operation might vary between projects.

#. Commit and tag as needed.

#. Start the `KMS CI job`_ with the parameters *JOB_RELEASE* **ENABLED** and *JOB_ONLY_KMS* **DISABLED**.

   The KMS CI job is a *Jenkins MultiJob Project*. If it fails at any stage, after fixing the cause of the error there is no need to start the job again from the beginning. Instead, you can resume the build from the point it was before the failure.

   For this, just open the latest build number that failed (with a red marker in the *Build History* panel at the left of the job page); in the description of the build, the action *Resume build* is available on the left side.

#. Wait until all packages get created and published correctly. Fix any issues that might appear.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0
   NEW_DEBIAN="<DebianRevision>"  # Eg.: 0kurento1

   cd kms-omni-build/

   # Set the new version.
   ./bin/set-versions.sh "$NEW_VERSION" --debian "$NEW_DEBIAN" \
       --release --commit --tag

   # Push committed changes.
   git submodule foreach 'git push --follow-tags'
   git push --follow-tags



Post-Release
------------

When all repos have been released, and CI jobs have finished successfully:

* Check that the Auto-Generated API Client JavaScript repos have been updated (which should happen as part of the CI jobs for all Kurento Media Server modules that contain KMD API Definition files, ``*.kmd``):

  - `kms-core`_ -> `kurento-client-core-js`_
  - `kms-elements`_ -> `kurento-client-elements-js`_
  - `kms-filters`_ -> `kurento-client-filters-js`_

  - `kms-chroma`_ -> `kurento-module-chroma-js`_
  - `kms-crowddetector`_ -> `kurento-module-crowddetector-js`_
  - `kms-datachannelexample`_ -> `kurento-module-datachannelexample-js`_
  - `kms-markerdetector`_ -> `kurento-module-markerdetector-js`_
  - `kms-platedetector`_ -> `kurento-module-platedetector-js`_
  - `kms-pointerdetector`_ -> `kurento-module-pointerdetector-js`_

* Open the `Nexus Sonatype Staging Repositories`_ section.
* Select **kurento** repository.
* Inspect **Content** to ensure they are as expected:

  - kurento-module-creator
  - kms-api-core
  - kms-api-elements
  - kms-api-filters
  - All of them must appear in the correct version, ``$NEW_VERSION``.

* **Close** repository.
* Wait a bit.
* **Refresh**.
* **Release** repository.
* Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

* Also, check that the JavaScript modules have been published by CI:

  - Open each module's page in NPM, and check that the latest version corresponds to the current release:

    - NPM: `kurento-client-core <https://www.npmjs.com/package/kurento-client-core>`__
    - NPM: `kurento-client-elements <https://www.npmjs.com/package/kurento-client-elements>`__
    - NPM: `kurento-client-filters <https://www.npmjs.com/package/kurento-client-filters>`__

  - If any of these are missing, it's probably due to the CI job not running (because the project didn't really contain any code difference from the previous version... happens sometimes when not all repos have changed since the last release). Open CI and run the jobs manually:

    - CI: `kurento_client_core_js_merged <https://ci.openvidu.io/jenkins/job/Development/job/kurento_client_core_js_merged/>`__
    - CI: `kurento_client_elements_js_merged <https://ci.openvidu.io/jenkins/job/Development/job/kurento_client_elements_js_merged/>`__
    - CI: `kurento_client_filters_js_merged <https://ci.openvidu.io/jenkins/job/Development/job/kurento_client_filters_js_merged/>`__



New Development
---------------

**After the whole release has been completed**, bump to a new development version. Do this by incrementing the *.PATCH* number and resetting the **Debian revision** to 1.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>"   # Eg.: 1.0.1
   NEW_DEBIAN="<DebianRevision>" # Eg.: 0kurento1

   cd kms-omni-build/

   # Set the new version.
   ./bin/set-versions.sh "$NEW_VERSION" --debian "$NEW_DEBIAN" \
       --new-development --commit

   # Push committed changes.
   git submodule foreach 'git push'

Then start the `KMS CI job`_ with the parameters *JOB_RELEASE* **DISABLED** and *JOB_ONLY_KMS* **DISABLED**.



Kurento JavaScript client
=========================

Release order:

* `kurento-jsonrpc-js`_
* `kurento-utils-js`_
* `kurento-client-js`_
* `kurento-tutorial-js`_
* `kurento-tutorial-node`_



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Check there are no uncommitted files.

#. Check latest changes from the main branch.

#. Set the new version. This operation might vary between projects.

#. Check there are no development versions in any of the dependencies.

#. Test the build. Make sure the code is in a working state.

   The most common issue is that the code is not properly formatted. To manually run the beautifier, do this:

   .. code-block:: shell

      npm install

      # To run beautifier over all files, modifying in-place:
      node_modules/.bin/grunt jsbeautifier::default

      # To run beautifier over a specific file:
      node_modules/.bin/grunt jsbeautifier::file:<FilePath>.js

#. Commit and tag as needed.

**All-In-One script**:

.. note::

   The *jq* command-line JSON processor must be installed.

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0

   function do_release {
       local COMMIT_MSG="Prepare release $NEW_VERSION"

       local PROJECTS=(
           kurento-jsonrpc-js
           kurento-utils-js
           kurento-client-js
           kurento-tutorial-js
           kurento-tutorial-node
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Check there are no uncommitted files.
           # Exclude JSON files, to allow re-running this function.
           git diff-index --quiet HEAD -- '!*.json' \
           || { echo "ERROR: Uncommitted files not allowed!"; return 1; }

           # Check latest changes from the main branch.
           # FIXME UPGRADE 16.04: Newer versions of git allow running `git pull --rebase --autostash`.
           git fetch && git rebase --autostash \
           || { echo "ERROR: Command failed: git pull"; return 1; }

           # Set the new version.
           ./bin/set-versions.sh "$NEW_VERSION" --release --git-add \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           # Check there are no development versions in any of the dependencies.
           grep -Fr --exclude-dir='*node_modules' --include='*.json' -e '-dev"' -e '"git+' \
           && { echo "ERROR: Development versions not allowed!"; return 1; }

           # Test the build.
           if [[ "$PROJECT" == "kurento-client-js" ]]; then
               # kurento-client-js depends on kurento-jsonrpc-js, so we'll use
               # `npm link` here to solve the dependency.
               # Use a custom Node prefix so `npm link` doesn't require root permissions.
               NPM_CONFIG_PREFIX=.npm npm link ../kurento-jsonrpc-js
           fi
           npm install || { echo "ERROR: Command failed: npm install"; return 1; }
           if [[ -x node_modules/.bin/grunt ]]; then
               node_modules/.bin/grunt jsbeautifier \
               && node_modules/.bin/grunt \
               && node_modules/.bin/grunt sync:bower \
               || { echo "ERROR: Command failed: grunt"; return 1; }
           fi

           popd
       done

       echo "Everything seems OK; proceed to commit and push"

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Commit all modified files.
           git commit -m "$COMMIT_MSG" \
           || { echo "ERROR: Command failed: git commit"; return 1; }

           # Push new commit(s).
           git push \
           || { echo "ERROR: Command failed: git push"; return 1; }

           #git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
           #&& git push origin "$NEW_VERSION" \
           #|| { echo "ERROR: Command failed: git tag"; return 1; }
           # NOTE: the CI jobs automatically tag the repos upon releases

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   (set -o xtrace; do_release)



Post-Release
------------

When all repos have been released, and CI jobs have finished successfully:

* Open the `Nexus Sonatype Staging Repositories`_ section.
* Select **kurento** repository.
* Inspect **Content** to ensure they are as expected:

  - kurento-jsonrpc-js
  - kurento-utils-js
  - kurento-client-js
  - All of them must appear in the correct version, ``$NEW_VERSION``.

* **Close** repository.
* Wait a bit.
* **Refresh**.
* **Release** repository.
* Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.



New Development
---------------

**After the whole release has been completed**, bump to a new development version. Do this by incrementing the *.PATCH* number.

**All-In-One script**:

.. note::

   The *jq* command-line JSON processor must be installed.

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>" # Eg.: 1.0.1

   function do_release {
       local COMMIT_MSG="Prepare for next development iteration"

       local PROJECTS=(
           kurento-jsonrpc-js
           kurento-utils-js
           kurento-client-js
           kurento-tutorial-js
           kurento-tutorial-node
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Set the new version.
           ./bin/set-versions.sh "$NEW_VERSION" --git-add \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           popd
       done

       echo "Everything seems OK; proceed to commit and push"

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Commit all modified files.
           git commit -m "$COMMIT_MSG" \
           || { echo "ERROR: Command failed: git commit"; return 1; }

           # Push new commit(s).
           git push \
           || { echo "ERROR: Command failed: git push"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   (set -o xtrace; do_release)



Kurento Java client
===================

Release order:

* `kurento-qa-pom`_
* `kurento-java`_
* `kurento-tutorial-java`_
* `kurento-tutorial-test`_



Preparation: kurento-java
-------------------------

If there have been changes in the API of Kurento Media Server modules (in the *.kmd* JSON files), update the corresponding versions in `kurento-parent-pom/pom.xml <https://github.com/Kurento/kurento-java/blob/1805889344933157e7a51574c38e4fd2fe921cc9/kurento-parent-pom/pom.xml#L78>`__:

.. code-block:: xml

   <version.kurento-chroma>1.1.0</version.kurento-chroma>
   <version.kurento-crowddetector>1.1.0</version.kurento-crowddetector>
   <version.kurento-markerdetector>1.1.0</version.kurento-markerdetector>
   <version.kurento-platedetector>1.1.0</version.kurento-platedetector>
   <version.kurento-pointerdetector>1.1.0</version.kurento-pointerdetector>

   <version.kurento-utils-js>1.1.0</version.kurento-utils-js>
   <version.kurento-maven-plugin>1.1.0</version.kurento-maven-plugin>

Doing this ensures that the Java client gets generated according to the latest versions of the API definitions.



Release steps
-------------

#. Choose the *final release version*, following the SemVer guidelines as explained in :ref:`dev-release-general`.

#. Check there are no uncommitted files.

#. Check latest changes from the main branch.

#. Set the new version. This operation might vary between projects.

   Order matters. *kurento-tutorial-java* and *kurento-tutorial-test* require that *kurento-java* has been installed locally (with ``mvn install``) before being able to change their version numbers programmatically with Maven.

#. Check there are no development versions in any of the dependencies.

   In *kurento-java*, all dependencies are defined as properties in the file ``kurento-parent-pom/pom.xml``.

#. Test the build. Make sure the code is in a working state.

   The profile '*kurento-release*' is used to enforce no development versions are present.

#. Commit and tag as needed.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<ReleaseVersion>" # Eg.: 1.0.0

   function do_release {
       local COMMIT_MSG="Prepare release $NEW_VERSION"

       local PROJECTS=(
           # FIXME: The interaction between this and kurento-java needs to
           # be addressed in the CI jobs. Probably copying the JAR artifacts.
           #kurento-qa-pom

           kurento-java
           kurento-tutorial-java

           # FIXME tests fail because Kurento Test Framework needs improvements
           #kurento-tutorial-test
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Check there are no uncommitted files.
           # Exclude XML files, to allow re-running this function.
           git diff-index --quiet HEAD -- '!*.xml' \
           || { echo "ERROR: Uncommitted files not allowed!"; return 1; }

           # Check latest changes from the main branch.
           # FIXME UPGRADE 16.04: Newer versions of git allow running `git pull --rebase --autostash`.
           git fetch && git rebase --autostash \
           || { echo "ERROR: Command failed: git pull"; return 1; }

           # Set the new version.
           ./bin/set-versions.sh "$NEW_VERSION" --release --git-add \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           # Check there are no development versions in any of the dependencies.
           grep -Fr --include='pom.xml' -e '-SNAPSHOT' \
           && { echo "ERROR: Development versions not allowed!"; return 1; }

           # Test the build.
           # Also install the project into local cache; this allows the next
           # projects to update their parent version.
           # * Build and run tests.
           # * Do not use `-U` because for each project we want Maven to find
           #   the locally installed artifacts from previous $PROJECT.
           mvn clean install -Dmaven.test.skip=false -Pkurento-release \
           || { echo "ERROR: Command failed: mvn clean install"; return 1; }

           popd
       done

       echo "Everything seems OK; proceed to commit and push"

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Commit all modified files.
           git commit -m "$COMMIT_MSG" \
           || { echo "ERROR: Command failed: git commit"; return 1; }

           # Push new commit(s).
           git push \
           || { echo "ERROR: Command failed: git push"; return 1; }

           #git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
           #&& git push origin "$NEW_VERSION" \
           #|| { echo "ERROR: Command failed: git tag"; return 1; }
           # NOTE: the CI jobs automatically tag the repos upon releases

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   (set -o xtrace; do_release)



Post-Release
------------

When all repos have been released, and CI jobs have finished successfully:

* Open the `Nexus Sonatype Staging Repositories`_ section.
* Select **kurento** repository.
* Inspect **Content** to ensure they are as expected:

  - kurento-client
  - kurento-commons
  - kurento-integration-tests
  - kurento-java
  - kurento-jsonrpc
  - kurento-jsonrpc-client
  - kurento-jsonrpc-client-jetty
  - kurento-jsonrpc-server
  - kurento-parent-pom
  - kurento-repository (ABANDONED)
  - kurento-repository-client (ABANDONED)
  - kurento-repository-internal (ABANDONED)
  - kurento-test
  - All of them must appear in the correct version, ``$NEW_VERSION``.

* **Close** repository.
* Wait a bit.
* **Refresh**.
* **Release** repository.
* Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.



New Development
---------------

.. warning::

   You should wait for a full nightly run of the Kurento Media Server pipeline, so the next development packages become available from KMS API modules: *kms-api-core*, *kms-api-elements*, and *kms-api-filters*. This way, the properties in ``kurento-parent-pom/pom.xml`` will get updated to the latest SNAPSHOT version.

**After the whole release has been completed**, bump to a new development version. Do this by incrementing the *.PATCH* number.

**All-In-One script**:

.. code-block:: shell

   # Change here.
   NEW_VERSION="<NextVersion>-SNAPSHOT" # Eg.: 1.0.1

   function do_release {
       local COMMIT_MSG="Prepare for next development iteration"

       local PROJECTS=(
           # FIXME: The interaction between this and kurento-java needs to
           # be addressed in the CI jobs. Probably copying the JAR artifacts.
           #kurento-qa-pom

           kurento-java

           # Do nothing; tutorials are left depending on release versions.
           #kurento-tutorial-java
           #kurento-tutorial-test
       )

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Set the new version.
           ./bin/set-versions.sh "$NEW_VERSION" --git-add \
           || { echo "ERROR: Command failed: set-versions"; return 1; }

           # Install the project.
           # * Skip building and running tests.
           # * Do not use `-U` because for each project we want Maven to find
           #   the locally installed artifacts from previous $PROJECT.
           mvn clean install -Dmaven.test.skip=true \
           || { echo "ERROR: Command failed: mvn clean install"; return 1; }

           popd
       done

       echo "Everything seems OK; proceed to commit and push"

       for PROJECT in "${PROJECTS[@]}"; do
           pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }

           # Commit all modified files.
           git commit -m "$COMMIT_MSG" \
           || { echo "ERROR: Command failed: git commit"; return 1; }

           # Push new commit(s).
           git push \
           || { echo "ERROR: Command failed: git push"; return 1; }

           popd
       done

       echo "Done!"
   }

   # Run in a subshell where all commands are traced.
   (set -o xtrace; do_release)



Docker images
=============

A new set of development images is deployed to `Kurento Docker Hub`_ on each nightly build. Besides, a release version will be published as part of the CI jobs chain when the `KMS CI job`_ is triggered.

The repository `kurento-docker`_ contains *Dockerfile*s for all the `Kurento Docker images`_, however this repo shouldn't be tagged, because it is essentially a "multi-repo" and the tags would be meaningless (because *which one of the sub-dirs would the tag apply to?*).



Kurento documentation
=====================

The documentation scripts will download both Java and JavaScript clients, generate HTML Javadoc / Jsdoc pages from them, and embed everything into a `static section <https://doc-kurento.readthedocs.io/en/latest/features/kurento_client.html#reference-documentation>`__.

For this reason, the documentation must be built only after all the other modules have been released.

#. Write the Release Notes in ``doc-kurento/source/project/relnotes/``.

#. Ensure that the whole nightly CI chain works:

   Job *doc-kurento* -> job *doc-kurento-readthedocs* -> `New build at Read the Docs`_.

#. Edit `VERSIONS.conf.sh`_ to set all relevant version numbers: version of the documentation itself, and all referred modules and client libraries.

   These numbers can be different because not all of the Kurento projects are necessarily released with the same frequency. Check each one of the Kurento repositories to verify what is the latest version of each one, and put it in the corresponding variable:

   - ``[VERSION_DOC]``: The docs version shown to readers. Normally same as ``[VERSION_KMS]``.
   - ``[VERSION_KMS]``: Repo `kurento-media-server`_.
   - ``[VERSION_CLIENT_JAVA]``: Repo `kurento-java`_.
   - ``[VERSION_CLIENT_JS]``: Repo `kurento-client-js`_.
   - ``[VERSION_UTILS_JS]``: Repo `kurento-utils-js`_.
   - ``[VERSION_TUTORIAL_JAVA]``: Repo `kurento-tutorial-java`_.
   - ``[VERSION_TUTORIAL_JS]``: Repo `kurento-tutorial-js`_.
   - ``[VERSION_TUTORIAL_NODE]``: Repo `kurento-tutorial-node`_.

#. In *VERSIONS.conf.sh*, set *VERSION_RELEASE* to **true**. Remember to set it again to *false* after the release, when starting a new development iteration.

#. Test the build locally, check everything works.

   .. code-block:: shell

      make html

   Note that the JavaDoc and JsDoc pages won't be generated locally if you don't have your system prepared to do so; also there are some Sphinx constructs or plugins that might fail if you don't have them ready to use, but the Read the Docs servers have them so they should end up working fine.

#. Git add, commit, and push. Trigger a nightly build, where you can **check the result** of the documentation builds to have an idea of how the final release build will end up looking like, at https://doc-kurento.readthedocs.io/en/latest/.

   .. code-block:: shell

      # `--all` to include possibly deleted files.
      git add --all \
          VERSIONS.conf.sh \
          source/project/relnotes/ \
      && git commit -m "$COMMIT_MSG" \
      && git push \
      || echo "ERROR: Command failed: git"

#. Run the `doc-kurento CI job`_ with the parameter *JOB_RELEASE* **ENABLED**.

#. CI automatically tags Release versions in both Read the Docs source repos `doc-kurento`_ and `doc-kurento-readthedocs`_, so the release will show up in the Read the Docs dashboard.

   .. note::

      If you made a mistake and want to re-create the git tag with a different commit, remember that the re-tagging must be done manually in both *doc-kurento* and *doc-kurento-readthedocs* repos. Read the Docs CI servers will read the latter one to obtain the documentation sources and release tags.

#. Open `Read the Docs Builds`_. If the new version hasn't been detected and built, do it manually: use the *Build Version* button to force a build of the *latest* version. Doing this, Read the Docs will "realize" that there is a new tagged release version of the documentation in the *doc-kurento-readthedocs* repo.

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set *VERSION_RELEASE* to **false**. Now, create a Release Notes document template where to write changes that will accumulate for the next release.

   **All-In-One** script:

   .. code-block:: shell

      # Change here.
      NEW_VERSION="<NextVersion>" # Eg.: 1.0.1

      function do_release {
          local COMMIT_MSG="Prepare for next development iteration"

          # Set [VERSION_RELEASE]="false"
          sed -r -i 's/\[VERSION_RELEASE\]=.*/[VERSION_RELEASE]="false"/' VERSIONS.conf.sh \
          || { echo "ERROR: Command failed: sed"; return 1; }

          # Set [VERSION_DOC]
          local VERSION_DOC="${NEW_VERSION}-dev"
          sed -r -i "s/\[VERSION_DOC\]=.*/[VERSION_DOC]=\"$VERSION_DOC\"/" VERSIONS.conf.sh \
          || { echo "ERROR: Command failed: sed"; return 2; }

          # Add a new Release Notes document
          local RELNOTES_NAME="v${NEW_VERSION//./_}"
          cp source/project/relnotes/v0_TEMPLATE.rst \
              "source/project/relnotes/${RELNOTES_NAME}.rst" \
          && sed -i "s/1.2.3/${NEW_VERSION}/" \
              "source/project/relnotes/${RELNOTES_NAME}.rst" \
          && sed -i "8i\   $RELNOTES_NAME" \
              source/project/relnotes/index.rst \
          || { echo "ERROR: Command failed: sed"; return 3; }

          git add \
              VERSIONS.conf.sh \
              source/project/relnotes/ \
          && git commit -m "$COMMIT_MSG" \
          && git push \
          || { echo "ERROR: Command failed: git"; return 4; }

          echo "Done!"
      }

      # Run in a subshell where all commands are traced
      (set -o xtrace; do_release)



.. Kurento links

.. _kurento-media-server/CHANGELOG.md: https://github.com/Kurento/kurento-media-server/blob/master/CHANGELOG.md
.. _kms-omni-build/bin/set-versions.sh: https://github.com/Kurento/kms-omni-build/blob/master/bin/set-versions.sh
.. _Kurento Docker Hub: https://hub.docker.com/u/kurento
.. _Kurento Docker images: https://hub.docker.com/r/kurento/kurento-media-server
.. _kurento-docker: https://github.com/Kurento/kurento-docker
.. _KMS CI job: https://ci.openvidu.io/jenkins/job/Development/job/00_KMS_BUILD_ALL/
.. _doc-kurento CI job: https://ci.openvidu.io/jenkins/job/Development/job/kurento_doc_merged/
.. _doc-kurento: https://github.com/Kurento/doc-kurento
.. _doc-kurento-readthedocs: https://github.com/Kurento/doc-kurento-readthedocs
.. _VERSIONS.conf.sh: https://github.com/Kurento/doc-kurento/blob/e021a6c98bcea4db351faf423e90b64b8aa977f6/VERSIONS.conf.sh



.. GitHub links
.. _jsoncpp: https://github.com/Kurento/jsoncpp
.. _libsrtp: https://github.com/Kurento/libsrtp
.. _openh264: https://github.com/Kurento/openh264
.. _openh264-gst-plugin: https://github.com/Kurento/openh264-gst-plugin
.. _libusrsctp: https://github.com/Kurento/libusrsctp
.. _gstreamer: https://github.com/Kurento/gstreamer
.. _gst-plugins-base: https://github.com/Kurento/gst-plugins-base
.. _gst-plugins-good: https://github.com/Kurento/gst-plugins-good
.. _gst-plugins-bad: https://github.com/Kurento/gst-plugins-bad
.. _gst-plugins-ugly: https://github.com/Kurento/gst-plugins-ugly
.. _gst-libav: https://github.com/Kurento/gst-libav
.. _openwebrtc-gst-plugins: https://github.com/Kurento/openwebrtc-gst-plugins
.. _libnice: https://github.com/Kurento/libnice

.. _kurento-module-creator: https://github.com/Kurento/kurento-module-creator
.. _kurento-maven-plugin: https://github.com/Kurento/kurento-maven-plugin
.. _kms-cmake-utils: https://github.com/Kurento/kms-cmake-utils
.. _kms-jsonrpc: https://github.com/Kurento/kms-jsonrpc
.. _kms-core: https://github.com/Kurento/kms-core
.. _kms-elements: https://github.com/Kurento/kms-elements
.. _kms-filters: https://github.com/Kurento/kms-filters
.. _kurento-media-server: https://github.com/Kurento/kurento-media-server
.. _kms-chroma: https://github.com/Kurento/kms-chroma
.. _kms-crowddetector: https://github.com/Kurento/kms-crowddetector
.. _kms-datachannelexample: https://github.com/Kurento/kms-datachannelexample
.. _kms-markerdetector: https://github.com/Kurento/kms-markerdetector
.. _kms-platedetector: https://github.com/Kurento/kms-platedetector
.. _kms-pointerdetector: https://github.com/Kurento/kms-pointerdetector

.. _kurento-client-core-js: https://github.com/Kurento/kurento-client-core-js
.. _kurento-client-elements-js: https://github.com/Kurento/kurento-client-elements-js
.. _kurento-client-filters-js: https://github.com/Kurento/kurento-client-filters-js
.. _kurento-module-chroma-js: https://github.com/Kurento/kurento-module-chroma-js
.. _kurento-module-crowddetector-js: https://github.com/Kurento/kurento-module-crowddetector-js
.. _kurento-module-datachannelexample-js: https://github.com/Kurento/kurento-module-datachannelexample-js
.. _kurento-module-markerdetector-js: https://github.com/Kurento/kurento-module-markerdetector-js
.. _kurento-module-platedetector-js: https://github.com/Kurento/kurento-module-platedetector-js
.. _kurento-module-pointerdetector-js: https://github.com/Kurento/kurento-module-pointerdetector-js

.. _kurento-jsonrpc-js: https://github.com/Kurento/kurento-jsonrpc-js
.. _kurento-utils-js: https://github.com/Kurento/kurento-utils-js
.. _kurento-client-js: https://github.com/Kurento/kurento-client-js
.. _kurento-tutorial-js: https://github.com/Kurento/kurento-tutorial-js
.. _kurento-tutorial-node: https://github.com/Kurento/kurento-tutorial-node

.. _kurento-qa-pom: https://github.com/Kurento/kurento-qa-pom
.. _kurento-java: https://github.com/Kurento/kurento-java
.. _kurento-tutorial-java: https://github.com/Kurento/kurento-tutorial-java
.. _kurento-tutorial-test: https://github.com/Kurento/kurento-tutorial-test



.. External links

.. _Debian Policy Manual: https://www.debian.org/doc/debian-policy/ch-controlfields.html#version
.. _Maven Versions Plugin: https://www.mojohaus.org/versions-maven-plugin/set-mojo.html#nextSnapshot
.. _Nexus Sonatype Staging Repositories: https://oss.sonatype.org/#stagingRepositories
.. _Semantic Versioning: https://semver.org/spec/v2.0.0.html#summary
.. _this Ask Ubuntu answer: https://askubuntu.com/questions/620533/what-is-the-meaning-of-the-xubuntuy-string-in-ubuntu-package-names/620539#620539
.. _Read the Docs Builds: https://readthedocs.org/projects/doc-kurento/builds/
.. _New build at Read the Docs: https://readthedocs.org/projects/doc-kurento/builds/
.. _Read the Docs Advanced Settings: https://readthedocs.org/dashboard/doc-kurento/advanced/
