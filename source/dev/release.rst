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

  - In Java (Maven) projects, development versions are indicated by the suffix ``-SNAPSHOT`` after the version number. Example: ``6.9.1-SNAPSHOT``.
  - In C/C++ (CMake) projects, development versions are indicated by the suffix ``-dev`` after the version number. Example: ``6.9.1-dev``.

  These suffixes must be removed for release, and then recovered again to resume development.

* All dependencies to development versions will be changed to a release version during the release procedure. Concerning people will be asked to choose an appropriate release version for each development dependency.

* Tags are named with the version number of the release. Example: ``6.9.0``.

* Contrary to the project version, the Debian package versions don't contain development suffixes, and should always be of the form ``1.2.3-0kurento1``:

  - The first part (*1.2.3*) is the project's **base version number**.

  - The second part (*0kurento1*) is the **Debian package revision**:

    - The number prefix (in this example: *0*) indicates the version relative to other same-name packages provided by the base system. When this number is *0*, it means that the package is original and only exists in Kurento, not in Debian or Ubuntu itself. This is typically the case for the projects owned or forked for the Kurento project.

    - The number suffix (in this example: *1*) means the number of times the same package has been re-packaged and re-published. *1* means that this is the *first* time a given project version was packaged.

    **Example**: Imagine that version *1.2.3* of your code is released for the first time. The full Debian package version will be: *1.2.3-0kurento1*. Then you realize the package doesn't install correctly in some machines, because of a bug in the package's post-install script. You fix it, and now it's time to re-publish the fixed package! But the project's source code itself has not changed at all, so it is not correct to increase the base version number: its version should still be *1.2.3*. The only changes have occurred in the packaging code itself (in ``/debian/`` dir). For this reason, the new package's full version will be *1.2.3-0kurento2*.

  Please check the `Debian Policy Manual`_ and `this Ask Ubuntu answer`_ for more information about the package versions.

* Kurento uses `Semantic Versioning`_. Whenever you need to decide what is going to be the *final release version* for a new release, try to follow the SemVer guidelines:

  .. code-block:: text

     Given a version number MAJOR.MINOR.PATCH, increment the:

     1. MAJOR version when you make incompatible API changes,
     2. MINOR version when you add functionality in a backwards-compatible manner, and
     3. PATCH version when you make backwards-compatible bug fixes.

  Please refer to https://semver.org/ for more information.

  **Example**

  If the last Kurento release was **6.8.2** (with for example Debian package version *6.8.2-0kurento3*, because maybe the package itself had bugs, so it has been published 3 times) then after release the project versions should have been left as **6.8.3-dev** (or *6.8.3-SNAPSHOT* for Java components).

  If the **next release** of Kurento only includes patches, then the next version number *6.8.3* is already good. However, maybe our release includes new functionality, which according to Semantic Versioning should be accompanied with a bump in the *minor* version number, so the next release version number should be *6.9.0*. The Debian package version is reset accordignly, so the full Debian version is **6.9.0-0kurento1**.

  If you are re-packaging an already published version, without changes in the project's code itself, then just increment the Debian package revision: *0kurento1* becomes *0kurento2*, and so on.



.. note::

   Made a mistake? Don't panic!

   Do not be afraid of applying some Git magic to solve mistakes during the release process. Here are some which can be useful:

   - How to remove a release tag?

     - Remove the local tag:

       .. code-block:: bash

          git tag --delete <TagName>

     - Remove the remote tag:

       .. code-block:: bash

          git push --delete origin <TagName>

   - How to push just a local tag?

     .. code-block:: bash

        git push origin <TagName>

   - How to ammend a commit and push it again?

     See: https://www.atlassian.com/git/tutorials/rewriting-history#git-commit--amend

     .. code-block:: bash

        # <Remove Tag>
        # <Amend>
        # <Create Tag>
        git push --force --follow-tags



.. warning::

   As of this writing, there is a mix of methods in the CI scripts (adm-scripts) when it comes to handle the release versions. The instructions in this document favor creating and pushing git tags manually in the developer's computer, however some projects also make use of the script ``kurento_check_version.sh``, which tries to detect when a project's version is *not* a development snapshot, then creates and pushes a git tag automatically. However if the tag alreeady exists (created manually by the developer), then the ``git tag`` command fails, and this script prints a warning message before continuing with its work.

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

For each project above:

1. Prepare release.
2. Push a new tag to Git.
3. Move to next development version.



Release steps
-------------

#. Decide what is going to be the *final release version*. For this, follow the upstream version and the SemVer guidelines, as explained above in :ref:`dev-release-general`.

#. Set the final release version, commit the results, and create a tag.

   .. code-block:: bash

      # Change these
      NEW_VERSION="<ReleaseVersion>"        # Eg.: 1.0.0
      NEW_DEBIAN="<DebianRevision>"         # Eg.: 0kurento1

      function do_release {
          local PACKAGE_VERSION="${NEW_VERSION}-${NEW_DEBIAN}"
          local COMMIT_MSG="Prepare release $PACKAGE_VERSION"

          local SNAPSHOT_ENTRY="* UNRELEASED"
          local RELEASE_ENTRY="* $COMMIT_MSG"

          gbp dch \
              --ignore-branch \
              --git-author \
              --spawn-editor=never \
              --new-version="$PACKAGE_VERSION" \
              \
              --release \
              --distribution="testing" \
              --force-distribution \
              \
              ./debian \
          || { echo "ERROR: Command failed: gbp dch"; return 1; }

          # First appearance of "UNRELEASED": Put our commit message
          sed -i "0,/${SNAPSHOT_ENTRY}/{s/${SNAPSHOT_ENTRY}/${RELEASE_ENTRY}/}" \
              ./debian/changelog \
          || { echo "ERROR: Command failed: sed"; return 2; }

          # Remaining appearances of "UNRELEASED" (if any): Delete line
          sed -i "/${SNAPSHOT_ENTRY}/d" \
              ./debian/changelog \
          || { echo "ERROR: Command failed: sed"; return 3; }

          git add debian/changelog \
          && git commit -m "$COMMIT_MSG" \
          && git tag -a -m "$COMMIT_MSG" "$PACKAGE_VERSION" \
          && git push --follow-tags \
          || { echo "ERROR: Command failed: git"; return 4; }
      }

      do_release

#. Follow on with releasing Kurento Media Server.

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set the next development version in all projects. To choose the next version number, increment the **Debian revision** number.

   The version number will only be changed when the fork gets updated from upstream code. When doing that, then change our version number so it matched the version number used in upstream.

   .. code-block:: bash

      # Change these
      NEW_VERSION="<NextVersion>"           # Eg.: 1.0.0
      NEW_DEBIAN="<NextDebianRevision>"     # Eg.: 0kurento2

      function do_release {
          local PACKAGE_VERSION="${NEW_VERSION}-${NEW_DEBIAN}"
          local COMMIT_MSG="Bump development version to $PACKAGE_VERSION"

          gbp dch \
                --ignore-branch \
                --git-author \
                --spawn-editor=never \
                --new-version="$PACKAGE_VERSION" \
                ./debian \
          || { echo "ERROR: Command failed: gbp dch"; return 1; }

          git add debian/changelog \
          && git commit -m "$COMMIT_MSG" \
          && git push \
          || { echo "ERROR: Command failed: git"; return 2; }
      }

      do_release



Kurento Media Server
====================

All KMS projects:

.. graphviz:: /images/graphs/dependencies-kms.dot
   :align: center
   :caption: Projects that are part of Kurento Media Server

Release order:

* `kurento-module-creator`_
* `kms-cmake-utils`_
* `kms-jsonrpc`_
* `kms-core`_
* `kms-elements`_
* `kms-filters`_
* `kurento-media-server`_
* `kms-chroma`_
* `kms-crowddetector`_
* `kms-datachannelexample`_
* `kms-platedetector`_
* `kms-pointerdetector`_

For each project above:

1. Prepare release.
2. Push a new tag to Git.
3. Move to next development version.



Preparation: kurento-module-creator
-----------------------------------

If **kurento-maven-plugin** is going to get also a new release, then edit the file *kurento-module-creator/src/main/templates/maven/model_pom_xml.ftl* to update the plugin version in the auto-generation template:

.. code-block:: xml

      <groupId>org.kurento</groupId>
      <artifactId>kurento-maven-plugin</artifactId>
   -  <version>6.8.2</version>
   +  <version>6.9.0</version>
      <executions>



Preparation: KMS API Java modules
---------------------------------

Test the KMS API Java module generation (local check).

.. code-block:: bash

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
   }

   do_release



Release steps
-------------

#. Review changes in all KMS sub-projects, and edit `kurento-media-server/CHANGELOG.md`_ to add them.

   You can use this command to get a list of commit messages since last release:

   .. code-block:: bash

      git log "$(git describe --tags --abbrev=0)"..HEAD --oneline

   Then add the new *CHANGELOG.md* for the upcoming release commit:

   .. code-block:: bash

      cd kurento-media-server
      git add CHANGELOG.md

#. Decide what is going to be the *final release version*. For this, follow the SemVer guidelines, as explained above in :ref:`dev-release-general`.

#. Set the final release version in all projects. Use the helper script `kms-omni-build/bin/set-versions.sh`_ to set version numbers, commit the results, and create a tag.

   .. code-block:: bash

      # Change these
      NEW_VERSION="<ReleaseVersion>"        # Eg.: 1.0.0
      NEW_DEBIAN="<DebianRevision>"         # Eg.: 0kurento1

      cd kms-omni-build
      ./bin/set-versions.sh "$NEW_VERSION" --debian "$NEW_DEBIAN" \
          --release --commit --tag

   Now push changes:

   .. code-block:: bash

      git submodule foreach 'git push --follow-tags'

#. It's also nice to update the git-submodule references of the all-in-one repo ``kms-omni-build``, and create a tag just like in all the other repos.

   .. code-block:: bash

      # Change this
      NEW_VERSION="<ReleaseVersion>"      # Eg.: 1.0.0

      COMMIT_MSG="Prepare release $NEW_VERSION"

      cd kms-omni-build
      git add . \
      && git commit -m "$COMMIT_MSG" \
      && git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
      && git push --follow-tags

#. Start the `KMS CI job`_ with the parameters ``JOB_RELEASE`` **ENABLED** and ``JOB_ONLY_KMS`` **DISABLED**.

#. Wait until all packages get created and published correctly. Fix any issues that appear.

   The KMS CI job is a *Jenkins MultiJob Project*. If it fails at any stage, after fixing the cause of the error it's not needed to start the job again from the beginning; instead, it is possible to resume the build from the point it was before the failure. For this, just open the latest build number that failed (with a red marker in the *Build History* panel at the left of the job page); in the description of the build, the action *Resume build* is available on the left side.

#. Check that the Auto-Generated API Client JavaScript repos have been updated (which should happen as part of the CI jobs for all Kurento Media Server modules that contain API Definition files, ``*.KMD``):

   - `kms-core`_ -> `kurento-client-core-js`_
   - `kms-elements`_ -> `kurento-client-elements-js`_
   - `kms-filters`_ -> `kurento-client-filters-js`_
   - `kms-chroma`_ -> `kurento-module-chroma-js`_
   - `kms-crowddetector`_ -> `kurento-module-crowddetector-js`_
   - `kms-datachannelexample`_ -> `kurento-module-datachannelexample-js`_
   - `kms-platedetector`_ -> `kurento-module-platedetector-js`_
   - `kms-pointerdetector`_ -> `kurento-module-pointerdetector-js`_

#. When all repos have been released, and CI jobs have finished successfully, publish the Java artifacts:

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repository.
   - Inspect **Content** to ensure they are as expected:

     - kurento-module-creator (if it was released)
     - kms-api-core
     - kms-api-elements
     - kms-api-filters
     - All of them must appear in the correct version, ``$NEW_VERSION``.

   - **Close** repository.
   - Wait a bit.
   - **Refresh**.
   - **Release** repository.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

#. Also, check that the JavaScript modules have been published by CI:

   - Open each module's page in NPM, and check that the latest version corresponds to the current release:

     - NPM: `kurento-client-core <https://www.npmjs.com/package/kurento-client-core>`__
     - NPM: `kurento-client-elements <https://www.npmjs.com/package/kurento-client-elements>`__
     - NPM: `kurento-client-filters <https://www.npmjs.com/package/kurento-client-filters>`__

   - If any of these are missing, it's probably due to the CI job not running (because the project didn't really contain any code difference from the previous version... happens sometimes when not all repos have changed since the last release). Open CI and run the jobs manually:

     - CI: `kurento_client_core_js_merged <https://ci.openvidu.io/jenkins/job/Development/job/kurento_client_core_js_merged/>`__
     - CI: `kurento_client_elements_js_merged <https://ci.openvidu.io/jenkins/job/Development/job/kurento_client_elements_js_merged/>`__
     - CI: `kurento_client_filters_js_merged <https://ci.openvidu.io/jenkins/job/Development/job/kurento_client_filters_js_merged/>`__

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set the next development version in all projects. To choose the next version number, reset the **Debian revision** number to *1*, and increment the **patch** number. Use the helper script *kms-omni-build/bin/set-versions.sh* to set version numbers and commit.

   .. code-block:: bash

      # Change these
      NEW_VERSION="<NextVersion>"           # Eg.: 1.0.1
      NEW_DEBIAN="<NextDebianRevision>"     # Eg.: 0kurento1

      cd kms-omni-build
      ./bin/set-versions.sh "$NEW_VERSION" --debian "$NEW_DEBIAN" \
          --new-development --commit

   Now push changes:

   .. code-block:: bash

      git submodule foreach 'git push'

#. Start the `KMS CI job`_ with the parameters ``JOB_RELEASE`` **DISABLED** and ``JOB_ONLY_KMS`` **DISABLED**.



Kurento JavaScript client
=========================

Release order:

* `kurento-jsonrpc-js`_
* `kurento-utils-js`_
* `kurento-client-js`_
* `kurento-tutorial-js`_
* `kurento-tutorial-node`_

For each project above:

1. Prepare release.
2. Push a new tag to Git.
3. Move to next development version.



Release steps
-------------

#. Decide what is going to be the *final release version*. For this, follow the SemVer guidelines, as explained above in :ref:`dev-release-general`.

#. Ensure there are no uncommited files.

   .. code-block:: bash

      git diff-index --quiet HEAD \
      || echo "ERROR: Uncommited files not allowed!"

#. Set the final release version in project and dependencies. This operation is done in different files, depending on the project:

   - ``kurento-jsonrpc-js/package.json``
   - ``kurento-utils-js/package.json``
   - ``kurento-client-js/package.json``
   - Each one in ``kurento-tutorial-js/**/bower.json``
   - Each one in ``kurento-tutorial-node/**/package.json``

#. Review all dependencies to remove development versions.

   This command can be used to search for all development versions:

   .. code-block:: bash

      grep . --exclude-dir='*node_modules' -Fr -e '-dev"' -e '"git+' \
      && echo "ERROR: Development versions not allowed!"

   For example: All dependencies to Kurento packages that point directly to their Git repos should be changed to point to a pinned SemVer number (or version range). Later, the Git URL can be restored for the next development iteration.

#. Test the build, to make sure the code is in a working state.

   .. code-block:: bash

      npm install
      if [[ -x node_modules/.bin/grunt ]]; then
          node_modules/.bin/grunt jsbeautifier \
          && node_modules/.bin/grunt \
          && node_modules/.bin/grunt sync:bower \
          || echo "ERROR: Command failed: npm"
      fi

   To manually run the beautifier, do this:

   .. code-block:: bash

      npm install

      # To run beautifier over all files, modifying in-place:
      node_modules/.bin/grunt jsbeautifier::default

      # To run beautifier over a specific file:
      node_modules/.bin/grunt jsbeautifier::file:<FilePath>.js

   Some times it happens that Grunt needs to be run a couple of times until it ends without errors.

#. **All-In-One** script:

   .. note::

      You'll need to install the **jq** command-line JSON processor.

   .. code-block:: bash

      # Change this
      NEW_VERSION="<ReleaseVersion>"        # Eg.: 1.0.0

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
              git stash

              git pull --rebase \
              || { echo "ERROR: Command failed: git pull"; return 2; }

              # Ensure there are no uncommited files
              git diff-index --quiet HEAD \
              || { echo "ERROR: Uncommited files not allowed!"; return 3; }

              # Set the final release version in project and dependencies
              JQ_PROGRAM="$(mktemp)"
              tee "$JQ_PROGRAM" >/dev/null <<EOF
      # This is a program for the "jq" command-line JSON processor
      # The last 4 rules are specifically for kurento-client-js
      if .version? then
          .version = "$NEW_VERSION"
      else . end
      | if .dependencies."kurento-client-core"? then
          .dependencies."kurento-client-core" = "$NEW_VERSION"
      else . end
      | if .dependencies."kurento-client-elements"? then
          .dependencies."kurento-client-elements" = "$NEW_VERSION"
      else . end
      | if .dependencies."kurento-client-filters"? then
          .dependencies."kurento-client-filters" = "$NEW_VERSION"
      else . end
      | if .dependencies."kurento-jsonrpc"? then
          .dependencies."kurento-jsonrpc" = "$NEW_VERSION"
      else . end
      EOF
              find . -path '*node_modules' -prune , -name '*.json' | while read FILE; do
                  echo "Process file: $(realpath "$FILE")"

                  TEMP="$(mktemp)"
                  jq --from-file "$JQ_PROGRAM" "$FILE" >"$TEMP" \
                  && mv --update "$TEMP" "$FILE" \
                  || { echo "ERROR: Command failed: jq"; return 4; }

                  git add "$FILE"
              done

              # Review all dependencies to remove development versions
              grep . --exclude-dir='*node_modules' -Fr -e '-dev"' -e '"git+' \
              && { echo "ERROR: Development versions not allowed!"; return 5; }

              # Test the build
              npm install
              if [[ -x node_modules/.bin/grunt ]]; then
                  node_modules/.bin/grunt jsbeautifier \
                  && node_modules/.bin/grunt \
                  && node_modules/.bin/grunt sync:bower \
                  || { echo "ERROR: Command failed: npm"; return 6; }
              fi

              git stash pop
              popd
          done

          # Everything seems OK so proceed to commit and push
          for PROJECT in "${PROJECTS[@]}"; do
              pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 7; }

              git commit -m "$COMMIT_MSG" \
              && git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
              && git push --follow-tags \
              || { echo "ERROR: Command failed: git"; return 8; }

              popd
          done
      }

      do_release

#. When all repos have been released, and CI jobs have finished successfully,

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repository.
   - Inspect **Content** to ensure they are as expected:

     - kurento-jsonrpc-js
     - kurento-utils-js
     - kurento-client-js
     - All of them must appear in the correct version, ``$NEW_VERSION``.

   - **Close** repository.
   - Wait a bit.
   - **Refresh**.
   - **Release** repository.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set the next development version in all projects. To choose the next version number, increment the **patch** number and add "*-dev*".

   **All-In-One** script:

   .. code-block:: bash

      # Change this
      NEW_VERSION="<NextVersion>-dev"           # Eg.: 1.0.1-dev

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
              git stash

              git pull --rebase \
              || { echo "ERROR: Command failed: git pull"; return 2; }

              # Set the next development version in project and dependencies
              JQ_PROGRAM="$(mktemp)"
              tee "$JQ_PROGRAM" >/dev/null <<EOF
      # This is a program for the "jq" command-line JSON processor
      # The last 4 rules are specifically for kurento-client-js
      if .version? then
          .version = "$NEW_VERSION"
      else . end
      | if .dependencies."kurento-client-core"? then
          .dependencies."kurento-client-core" = "git+https://github.com/Kurento/kurento-client-core-js.git"
      else . end
      | if .dependencies."kurento-client-elements"? then
          .dependencies."kurento-client-elements" = "git+https://github.com/Kurento/kurento-client-elements-js.git"
      else . end
      | if .dependencies."kurento-client-filters"? then
          .dependencies."kurento-client-filters" = "git+https://github.com/Kurento/kurento-client-filters-js.git"
      else . end
      | if .dependencies."kurento-jsonrpc"? then
          .dependencies."kurento-jsonrpc" = "git+https://github.com/Kurento/kurento-jsonrpc-js.git"
      else . end
      EOF
              find . -path '*node_modules' -prune , -name '*.json' | while read FILE; do
                  echo "Process file: $(realpath "$FILE")"

                  TEMP="$(mktemp)"
                  jq --from-file "$JQ_PROGRAM" "$FILE" >"$TEMP" \
                  && mv --update "$TEMP" "$FILE" \
                  || { echo "ERROR: Command failed: jq"; return 3; }

                  git add "$FILE"
              done

              git stash pop
              popd
          done

          # Everything seems OK so proceed to commit and push
          for PROJECT in "${PROJECTS[@]}"; do
              pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 4; }

              git commit -m "$COMMIT_MSG" \
              && git push \
              || { echo "ERROR: Command failed: git"; return 5; }

              popd
          done
      }

      do_release



Kurento Maven plugin
====================

#. Review changes in all KMS sub-projects, and edit *changelog* to add them.

   You can use this command to get a list of commit messages since last release:

   .. code-block:: bash

      git log "$(git describe --tags --abbrev=0)"..HEAD --oneline

#. Decide what is going to be the *final release version*. For this, follow the SemVer guidelines, as explained above in :ref:`dev-release-general`.

#. Set the final release version in *pom.xml*. Remove "*-SNAPSHOT*".

   .. code-block:: xml

         <groupId>org.kurento</groupId>
         <artifactId>kurento-maven-plugin</artifactId>
      -  <version>1.2.3-SNAPSHOT</version>
      +  <version>1.2.3</version>
         <packaging>maven-plugin</packaging>

#. Git add, commit, tag, and push.

   .. code-block:: bash

      # Change this
      NEW_VERSION="<ReleaseVersion>"        # Eg.: 1.0.0

      COMMIT_MSG="Prepare release $NEW_VERSION"

      cd kurento-maven-plugin
      git add pom.xml changelog \
      && git commit -m "$COMMIT_MSG" \
      && git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
      && git push --follow-tags  \
      || echo "ERROR: Command failed: git"

#. The CI jobs should start automatically; some tests are run as a result of this commit, so you should wait for their completion.

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set the next development version in all projects. To choose the next version number, increment the **patch** number and add "*-SNAPSHOT*".

   .. code-block:: xml

         <groupId>org.kurento</groupId>
         <artifactId>kurento-maven-plugin</artifactId>
      -  <version>1.2.3</version>
      +  <version>1.2.4-SNAPSHOT</version>
         <packaging>maven-plugin</packaging>

6. Git add, commit, and push.

   .. code-block:: bash

      COMMIT_MSG="Prepare for next development iteration"

      cd kurento-maven-plugin
      git add pom.xml \
      && git commit -m "$COMMIT_MSG" \
      && git push \
      || echo "ERROR: Command failed: git"



Kurento Java client
===================

Release order:

* `kurento-qa-pom`_
* `kurento-java`_
* `kurento-tutorial-java`_
* `kurento-tutorial-test`_

For each project above:

1. Prepare release.
2. Push a new tag to Git.
3. Move to next development version.



Preparation: kurento-java
-------------------------

If there have been changes in the API of Kurento Media Server modules (in the ``.KMD`` JSON files), update the corresponding versions in `kurento-parent-pom/pom.xml <https://github.com/Kurento/kurento-java/blob/70f27b8baeaf254ddcded9566171144811ab1a19/kurento-parent-pom/pom.xml#L75>`__:

.. code-block:: xml

       <properties>
   -   <version.kms-api-core>6.8.2</version.kms-api-core>
   -   <version.kms-api-elements>6.8.2</version.kms-api-elements>
   -   <version.kms-api-filters>6.8.2</version.kms-api-filters>
   +   <version.kms-api-core>6.9.0</version.kms-api-core>
   +   <version.kms-api-elements>6.9.0</version.kms-api-elements>
   +   <version.kms-api-filters>6.9.0</version.kms-api-filters>

Doing this ensures that the Java client gets generated according to the latest versions of the API definitions.

Similarly, update the version numbers of any other Kurento project that has been updated:

.. code-block:: xml

   <version.kurento-utils-js>6.7.0</version.kurento-utils-js>
   <version.kurento-maven-plugin>6.7.0</version.kurento-maven-plugin>

   <version.kurento-chroma>6.6.0</version.kurento-chroma>
   <version.kurento-crowddetector>6.6.0</version.kurento-crowddetector>
   <version.kurento-platedetector>6.6.0</version.kurento-platedetector>
   <version.kurento-pointerdetector>6.6.0</version.kurento-pointerdetector>



Release steps
-------------

#. Decide what is going to be the *final release version*. For this, follow the SemVer guidelines, as explained above in :ref:`dev-release-general`.

#. Ensure there are no uncommited files.

   .. code-block:: bash

      git diff-index --quiet HEAD \
      || echo "ERROR: Uncommited files not allowed!"

#. Set the final release version in project and dependencies. This operation varies between projects.

   .. note::

      *kurento-tutorial-java* and *kurento-tutorial-test* require that *kurento-java* has been installed locally (with ``mvn install``) before being able to change their version numbers programmatically with Maven.

#. Review all dependencies to remove development versions.

   .. note::

      In *kurento-java*, all dependencies are defined as properties in the file *kurento-parent-pom/pom.xml*.

   This command can be used to search for all development versions:

   .. code-block:: bash

      grep . --include='pom.xml' -Fr -e '-SNAPSHOT' \
      && echo "ERROR: Development versions not allowed!"

#. Test the build, to make sure the code is in a working state.

   .. note::

      The profile '*kurento-release*' is used to enforce no development versions are present.

   .. code-block:: bash

      mvn -U clean install -Dmaven.test.skip=false -Pkurento-release \
      || echo "ERROR: Command failed: mvn clean install"

#. **All-In-One** script:

   .. note::

      Use ``mvn --batch-mode`` if you copy this to an actual script.

   .. code-block:: bash

      # Change this
      NEW_VERSION="<ReleaseVersion>"        # Eg.: 1.0.1
      KMS_VERSION="<KmsApiVersion>"         # Eg.: 1.0.0

      function do_release {
          local COMMIT_MSG="Prepare release $NEW_VERSION"

          local PROJECTS=(
              kurento-qa-pom
              kurento-java
              kurento-tutorial-java
              kurento-tutorial-test
          )

          for PROJECT in "${PROJECTS[@]}"; do
              pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }
              git stash

              git pull --rebase \
              || { echo "ERROR: Command failed: git pull"; return 2; }

              # Ensure there are no uncommited files
              git diff-index --quiet HEAD \
              || { echo "ERROR: Uncommited files not allowed!"; return 3; }

              # Set the final release version in project and dependencies
              if [[ "$PROJECT" == "kurento-qa-pom" ]]; then
                  mvn \
                      versions:set \
                      -DgenerateBackupPoms=false \
                      -DnewVersion="$NEW_VERSION" \
                  || { echo "ERROR: Command failed: mvn versions:set"; return 4; }
              elif [[ "$PROJECT" == "kurento-java" ]]; then
                  mvn --file kurento-parent-pom/pom.xml \
                      versions:set \
                      -DgenerateBackupPoms=false \
                      -DnewVersion="$NEW_VERSION" \
                  || { echo "ERROR: Command failed: mvn versions:set"; return 5; }

                  mvn --file kurento-parent-pom/pom.xml \
                      versions:set-property \
                      -DgenerateBackupPoms=false \
                      -Dproperty=version.kms-api-core \
                      -DnewVersion="$KMS_VERSION"
                  mvn --file kurento-parent-pom/pom.xml \
                      versions:set-property \
                      -DgenerateBackupPoms=false \
                      -Dproperty=version.kms-api-elements \
                      -DnewVersion="$KMS_VERSION"
                  mvn --file kurento-parent-pom/pom.xml \
                      versions:set-property \
                      -DgenerateBackupPoms=false \
                      -Dproperty=version.kms-api-filters \
                      -DnewVersion="$KMS_VERSION"
              else # kurento-tutorial-java, kurento-tutorial-test
                  mvn \
                      versions:update-parent \
                      -DgenerateBackupPoms=false \
                      -DparentVersion="[${NEW_VERSION}]" \
                  || { echo "ERROR: Command failed: mvn versions:update-parent"; return 6; }

                  mvn -N \
                      versions:update-child-modules \
                      -DgenerateBackupPoms=false \
                  || { echo "ERROR: Command failed: mvn versions:update-child-modules"; return 7; }
              fi

              # Review all dependencies to remove development versions
              grep . --include='pom.xml' -Fr -e '-SNAPSHOT' \
              && echo "ERROR: Development versions not allowed!"

              # Test the build
              mvn -U clean install -Dmaven.test.skip=false -Pkurento-release \
              || { echo "ERROR: Command failed: mvn clean install"; return 8; }

              git stash pop
              popd
          done

          # Everything seems OK so proceed to commit and push
          for PROJECT in "${PROJECTS[@]}"; do
              pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 9; }

              ( git ls-files --modified | grep -E '/?pom.xml$' | xargs -r git add ) \
              && git commit -m "$COMMIT_MSG" \
              && git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
              && git push --follow-tags \
              || { echo "ERROR: Command failed: git"; return 10; }

              popd
          done
      }

      do_release

#. When all repos have been released, and CI jobs have finished successfully:

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repositories.
   - Inspect **Content** to ensure they are as expected: *kurento-java*, etc.
   - **Close repositories**.
   - Wait a bit.
   - **Refresh**.
   - **Release repositories**.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repository.
   - Inspect **Content** to ensure they are as expected:

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

   - **Close** repository.
   - Wait a bit.
   - **Refresh**.
   - **Release** repository.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set the next development version in all projects. To choose the next version number, increment the **patch** number and add "*-SNAPSHOT*".

   .. note::

      Maven can do this automatically with the `Maven Versions Plugin`_.

   .. note::

      You should wait for a full nightly run of the Kurento Media Server pipeline, so the next development packages become available from KMS API modules: *kms-api-core*, *kms-api-elements*, and *kms-api-filters*. This way, the properties in *kurento-parent-pom/pom.xml* will get updated to the latest SNAPSHOT version.

   **All-In-One** script:

   .. note::

      Use ``mvn --batch-mode`` if you copy this to an actual script.

   .. code-block:: bash

      function do_release {
          local COMMIT_MSG="Prepare for next development iteration"

          local PROJECTS=(
              kurento-qa-pom
              kurento-java
              kurento-tutorial-java
              kurento-tutorial-test
          )

          for PROJECT in "${PROJECTS[@]}"; do
              pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 1; }
              git stash

              # Set the next development version in project and dependencies
              if [[ "$PROJECT" == "kurento-qa-pom" ]]; then
                  mvn \
                      versions:set \
                      -DgenerateBackupPoms=false \
                      -DnextSnapshot=true \
                  || { echo "ERROR: Command failed: mvn versions:set"; return 2; }
              elif [[ "$PROJECT" == "kurento-java" ]]; then
                  mvn --file kurento-parent-pom/pom.xml \
                      versions:set \
                      -DgenerateBackupPoms=false \
                      -DnextSnapshot=true \
                  || { echo "ERROR: Command failed: mvn versions:set"; return 3; }

                  mvn --file kurento-parent-pom/pom.xml \
                      versions:update-property \
                      -DgenerateBackupPoms=false \
                      -Dproperty=version.kms-api-core \
                      -DallowSnapshots=true
                  mvn --file kurento-parent-pom/pom.xml \
                      versions:update-property \
                      -DgenerateBackupPoms=false \
                      -Dproperty=version.kms-api-elements \
                      -DallowSnapshots=true
                  mvn --file kurento-parent-pom/pom.xml \
                      versions:update-property \
                      -DgenerateBackupPoms=false \
                      -Dproperty=version.kms-api-filters \
                      -DallowSnapshots=true
              else # kurento-tutorial-java, kurento-tutorial-test
                  mvn \
                      versions:update-parent \
                      -DgenerateBackupPoms=false \
                      -DallowSnapshots=true \
                  || { echo "ERROR: Command failed: mvn versions:update-parent"; return 4; }

                  mvn -N \
                      versions:update-child-modules \
                      -DgenerateBackupPoms=false \
                      -DallowSnapshots=true \
                  || { echo "ERROR: Command failed: mvn versions:update-child-modules"; return 5; }
              fi

              # Test the build
              mvn -U clean install -Dmaven.test.skip=true \
              || { echo "ERROR: Command failed: mvn clean install"; return 6; }

              git stash pop
              popd
          done

          # Everything seems OK so proceed to commit and push
          for PROJECT in "${PROJECTS[@]}"; do
              pushd "$PROJECT" || { echo "ERROR: Command failed: pushd"; return 7; }

              ( git ls-files --modified | grep -E '/?pom.xml$' | xargs -r git add ) \
              && git commit -m "$COMMIT_MSG" \
              && git push \
              || { echo "ERROR: Command failed: git"; return 8; }

              popd
          done
      }

      do_release



Docker images
=============

A new set of development images is deployed to `Kurento Docker Hub`_ on each nightly build. Besides, a release version will be published as part of the CI jobs chain when the `KMS CI job`_ is triggered.

The repository `kurento-docker`_ contains *Dockerfile*s for all the `Kurento Docker images`_, however this repo shouldn't be tagged, because it is essentially a "multi-repo" and the tags would be meaningless (because *which one of the sub-dirs would the tag apply to?*).



Kurento documentation
=====================

The documentation scripts will download both Java and JavaScript clients, generate HTML Javadoc / Jsdoc pages from them, and embed everything into a `static section <https://doc-kurento.readthedocs.io/en/stable/features/kurento_client.html#reference-documentation>`__.

For this reason, the documentation must be built only after all the other modules have been released.

#. Write the Release Notes in *doc-kurento/source/project/relnotes/*.

#. Ensure that the whole nightly CI chain works:

   Job *doc-kurento* -> job *doc-kurento-readthedocs* -> `New build at ReadTheDocs`_.

#. Edit `VERSIONS.conf.sh`_ to set all relevant version numbers: version of the documentation itself, and all referred modules and client libraries.

   These numbers can be different because not all of the Kurento projects are necessarily released with the same frequency. Check each one of the Kurento repositories to verify what is the latest version of each one, and put it in the corresponding variable:

   - ``[VERSION_KMS]``: Repo `kurento-media-server`_.
   - ``[VERSION_CLIENT_JAVA]``: Repo `kurento-java`_.
   - ``[VERSION_CLIENT_JS]``: Repo `kurento-client-js`_.
   - ``[VERSION_UTILS_JS]``: Repo `kurento-utils-js`_.
   - ``[VERSION_TUTORIAL_JAVA]``: Repo `kurento-tutorial-java`_.
   - ``[VERSION_TUTORIAL_JS]``: Repo `kurento-tutorial-js`_.
   - ``[VERSION_TUTORIAL_NODE]``: Repo `kurento-tutorial-node`_.

#. In *VERSIONS.conf.sh*, set ``VERSION_RELEASE`` to ``true``. Remember to set it again to ``false`` after the release, when starting a new development iteration.

#. Test the build locally, check everything works.

   .. code-block:: bash

      make html

   Note that the JavaDoc and JsDoc pages won't be generated locally if you don't have your system prepared to do so; also there are some Sphinx constructs or plugins that might fail if you don't have them ready to use, but the ReadTheDocs servers have them so they should end up working fine.

   In any case, **always check the final result** of the intermediate documentation builds at https://doc-kurento.readthedocs.io/en/latest/, to have an idea of how the final release build will end up looking like.

#. Git add, commit, tag, and push.

#. **All-In-One** script:

   .. code-block:: bash

      # Change this
      NEW_VERSION="<ReleaseVersion>"        # Eg.: 1.0.0

      function do_release {
          local COMMIT_MSG="Prepare release $NEW_VERSION"

          # Set [VERSION_RELEASE]="true"
          sed -r -i 's/(VERSION_RELEASE.*)false/\1true/' VERSIONS.conf.sh \
          || { echo "ERROR: Command failed: sed"; return 1; }

          git add VERSIONS.conf.sh \
          && git add source/project/relnotes/v*.rst \
          && git commit -m "$COMMIT_MSG" \
          && git tag -a -m "$COMMIT_MSG" "$NEW_VERSION" \
          && git push --follow-tags \
          || { echo "ERROR: Command failed: git"; return 1; }
      }

      do_release

   .. note::

      If you made a mistake and want to re-create the git tag with a different commit, remember that the re-tagging must be done manually in both *doc-kurento* and *doc-kurento-readthedocs* repos. ReadTheDocs CI servers will read the later one to obtain the documentation sources and release tags.

#. Run the `doc-kurento CI job`_ with the parameter ``JOB_RELEASE`` **ENABLED**.

#. CI automatically tags Release versions in the ReadTheDocs source repo, `doc-kurento-readthedocs`_, so the release will show up as "*stable*" in ReadTheDocs.

#. Open `ReadTheDocs Builds`_. If the new version hasn't been detected and built, do it manually: use the *Build Version* button to force a build of the *latest* version.

   Doing this, ReadTheDocs will "realize" that there is a new tagged release version of the documentation, in the *doc-kurento-readthedocs* repo. After the build is finished, the new release version will be available for selection in the next step.

#. Open `ReadTheDocs Advanced Settings`_ and select the new version in the *Default Version* combo box.

   .. note::

      We don't set the *Default Version* field to "*stable*", because we want that the actual version number gets shown in the upper part of the side panel (below the Kurento logo, above the search box) when users open the documentation. If "*stable*" was selected here, then users would just see the word "*stable*" in the mentioned panel.

#. **AFTER THE WHOLE RELEASE HAS BEEN COMPLETED**: Set ``VERSION_RELEASE`` to ``false``. Now, create a Release Notes document template where to write changes that will accumulate for the next release.

   **All-In-One** script:

   .. code-block:: bash

      # Change this
      NEW_VERSION="<NextVersion>"           # Eg.: 1.0.1

      function do_release {
          local COMMIT_MSG="Prepare for next development iteration"

          # Set [VERSION_RELEASE]="false"
          sed -r -i 's/(VERSION_RELEASE.*)true/\1false/' VERSIONS.conf.sh \
          || { echo "ERROR: Command failed: sed"; return 1; }

          # Add a new Release Notes document
          local RELNOTES_NAME="v${NEW_VERSION//./_}"
          cp source/project/relnotes/v0_TEMPLATE.rst \
              "source/project/relnotes/${RELNOTES_NAME}.rst" \
          && sed -i "s/1.2.3/${NEW_VERSION}/" \
              "source/project/relnotes/${RELNOTES_NAME}.rst" \
          && sed -i "8i\   $RELNOTES_NAME" \
              source/project/relnotes/index.rst \
          || { echo "ERROR: Command failed: sed"; return 1; }

          git add VERSIONS.conf.sh \
          && git add source/project/relnotes/v*.rst \
          && git add source/project/relnotes/index.rst \
          && git commit -m "$COMMIT_MSG" \
          && git push \
          || { echo "ERROR: Command failed: git"; return 1; }
      }

      do_release



.. Kurento links

.. _kurento-media-server/CHANGELOG.md: https://github.com/Kurento/kurento-media-server/blob/master/CHANGELOG.md
.. _kms-omni-build/bin/set-versions.sh: https://github.com/Kurento/kms-omni-build/blob/master/bin/set-versions.sh
.. _Kurento Docker Hub: https://hub.docker.com/u/kurento
.. _Kurento Docker images: https://hub.docker.com/r/kurento/kurento-media-server
.. _kurento-docker: https://github.com/Kurento/kurento-docker
.. _KMS CI job: https://ci.openvidu.io/jenkins/job/Development/job/00_KMS_BUILD_ALL/
.. _doc-kurento CI job: https://ci.openvidu.io/jenkins/job/Development/job/kurento_doc_merged/
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
.. _kms-cmake-utils: https://github.com/Kurento/kms-cmake-utils
.. _kms-jsonrpc: https://github.com/Kurento/kms-jsonrpc
.. _kms-core: https://github.com/Kurento/kms-core
.. _kms-elements: https://github.com/Kurento/kms-elements
.. _kms-filters: https://github.com/Kurento/kms-filters
.. _kurento-media-server: https://github.com/Kurento/kurento-media-server
.. _kms-chroma: https://github.com/Kurento/kms-chroma
.. _kms-crowddetector: https://github.com/Kurento/kms-crowddetector
.. _kms-datachannelexample: https://github.com/Kurento/kms-datachannelexample
.. _kms-platedetector: https://github.com/Kurento/kms-platedetector
.. _kms-pointerdetector: https://github.com/Kurento/kms-pointerdetector

.. _kurento-client-core-js: https://github.com/Kurento/kurento-client-core-js
.. _kurento-client-elements-js: https://github.com/Kurento/kurento-client-elements-js
.. _kurento-client-filters-js: https://github.com/Kurento/kurento-client-filters-js
.. _kurento-module-chroma-js: https://github.com/Kurento/kurento-module-chroma-js
.. _kurento-module-crowddetector-js: https://github.com/Kurento/kurento-module-crowddetector-js
.. _kurento-module-datachannelexample-js: https://github.com/Kurento/kurento-module-datachannelexample-js
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
.. _doc-kurento-readthedocs: https://github.com/Kurento/doc-kurento-readthedocs/releases
.. _ReadTheDocs Builds: https://readthedocs.org/projects/doc-kurento/builds/
.. _New build at ReadTheDocs: https://readthedocs.org/projects/doc-kurento/builds/
.. _ReadTheDocs Advanced Settings: https://readthedocs.org/dashboard/doc-kurento/advanced/
