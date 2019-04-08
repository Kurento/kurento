==================
Release Procedures
==================

[WORK-IN-PROGRESS]

.. contents:: Table of Contents



Introduction
============

Kurento as a project spans across a multitude of different technologies and languages, each of them with their sets of conventions and *best practices*. This document aims to summarize all release procedures that apply to each one of the modules that compose the Kurento project. The main form of categorization is by technology type: C/C++ based modules, Java modules, JavaScript modules, and others.


.. _dev-release-general:

General considerations
======================

- Lists of projects in this document are sorted according to the repository lists given in :ref:`dev-code-repos`.

- Kurento projects to be released have supposedly been under development, and will have development version numbers:

  - In Java (Maven) projects, development versions are indicated by the suffix ``-SNAPSHOT`` after the version number. Example: ``6.7.0-SNAPSHOT``.
  - In C/C++ (CMake) projects, development versions are indicated by the suffix ``-dev`` after the version number. Example: ``6.7.0-dev``.

  These suffixes must be removed for release, and then recovered again to resume development.

- All dependencies to development versions will be changed to a release version during the release procedure. Concerning people will be asked to choose an appropriate release version for each development dependency.

- Tags are named with the version number of the release. Example: ``6.7.0``.

- Contrary to the project version, the Debian package versions don't contain development suffixes, and should always be of the form ``1.2.3-0kurento1``:

  - The first part (*1.2.3*) is the project's **base version number**.
  - The second part (*0kurento1*) is the **Debian package revision**. The first number (*0*) means that the package only exists in Kurento (not in Debian or Ubuntu); this is typically the case for the projects forked by Kurento. The rest (*kurento1*) means that this is the *1st* package released by Kurento for the corresponding base version.

  Please check the `Debian Policy Manual`_ and `this Ask Ubuntu answer`_ for more information about the package versions.

  .. note::

     Most Kurento fork packages have a *Debian package revision* that starts with *1* instead of *0*. This was due to a mistake, but changing it back to 0 would cause more problems than solutions so we're maintaining those until the projects get updated to a newer base version.

- Kurento uses `Semantic Versioning`_. Whenever you need to decide what is going to be the *definitive release version* for a new release, try to follow the SemVer guidelines:

  .. code-block:: text

     Given a version number MAJOR.MINOR.PATCH, increment the:

     1. MAJOR version when you make incompatible API changes,
     2. MINOR version when you add functionality in a backwards-compatible manner, and
     3. PATCH version when you make backwards-compatible bug fixes.

  Please refer to https://semver.org/ for more information.



Fork Repositories
=================

This graph shows the dependencies between forked projects used by Kurento:

.. graphviz:: /images/graphs/dependencies-forks.dot
   :align: center
   :caption: Projects forked by Kurento

Release order:

- jsoncpp
- libsrtp
- openh264
- usrsctp
- gstreamer
- gst-plugins-base
- gst-plugins-good
- gst-plugins-bad
- gst-plugins-ugly
- gst-libav
- openwebrtc-gst-plugins
- libnice

For each project above:

1. Prepare release.
2. Push a new tag to Git.
3. Move to next development version.



Release steps
-------------

#. Set the Debian package version, commit the results, and create a tag:

   .. code-block:: bash

      cd gst-plugins-bad

      # Edit these
      NEW_VERSION="0.1.15"
      NEW_DEBIAN="1kurento3"

      PACKAGE_VERSION="${NEW_VERSION}-${NEW_DEBIAN}"
      COMMIT_MSG="Prepare release $PACKAGE_VERSION"

      gbp dch \
          --ignore-branch \
          --git-author \
          --spawn-editor=never \
          --new-version="$PACKAGE_VERSION" \
          ./debian/

      SNAPSHOT_ENTRY="* UNRELEASED"
      RELEASE_ENTRY="* $COMMIT_MSG"

      # First appearance of 'UNRELEASED': Put our commit message
      sed --in-place --expression="0,/${SNAPSHOT_ENTRY}/{s/${SNAPSHOT_ENTRY}/${RELEASE_ENTRY}/}" \
          ./debian/changelog

      # Remaining appearances of 'UNRELEASED' (if any): Delete line
      sed --in-place --expression="/${SNAPSHOT_ENTRY}/d" \
          ./debian/changelog

      git add debian/changelog
      git commit -m "$COMMIT_MSG"
      git tag -a -m "$COMMIT_MSG" "$PACKAGE_VERSION"
      git push --follow-tags

#. Follow on with releasing Kurento Media Server.



Kurento Media Server
====================

All KMS projects:

.. graphviz:: /images/graphs/dependencies-kms.dot
   :align: center
   :caption: Projects that are part of Kurento Media Server

Release order:

- kurento-module-creator
- kms-cmake-utils
- kms-jsonrpc
- kms-core
- kms-elements
- kms-filters
- kurento-media-server

- kms-chroma
- kms-crowddetector
- kms-platedetector
- kms-pointerdetector

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

Test the KMS API Java module generation (local check):

.. code-block:: bash

   apt-get install --yes \
       kurento-module-creator \
       kms-cmake-utils \
       kms-jsonrpc-dev \
       kms-core-dev \
       kms-elements-dev \
       kms-filters-dev

   cd kms-omni-build

   for DIR in kms-core kms-elements kms-filters; do
       pushd "$DIR"
       mkdir build && cd build/
       cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE
       cd java/
       mvn --batch-mode clean install -Dmaven.test.skip=true
       popd
   done



Release steps
-------------

#. For all Kurento projects, edit *CHANGELOG.md* to add latest changes.

   Use this command to get a list of commit messages since last release:

   .. code-block:: bash

      git log "$(git describe --tags --abbrev=0)"..HEAD --oneline

#. Decide what is going to be the *definitive release version*. For this, follow the SemVer guidelines, as explained above in :ref:`dev-release-general`.

#. Set the definitive release version in all projects. Use the script `kms-omni-build/bin/set-versions.sh <https://github.com/Kurento/kms-omni-build/blob/master/bin/set-versions.sh>`__ to set version numbers, commit the results, and create a tag:

   .. code-block:: bash

      cd kms-omni-build
      ./bin/set-versions.sh <ReleaseVersion> --debian <DebianVersion> \
          --release --commit --tag

   **Example**

   If the last Kurento release was **6.9.0** (with e.g. Debian package version *6.9.0-0kurento3*, because it had been repackaged 3 times) then after release the project versions should have been left as **6.9.1-dev** (or *6.9.1-SNAPSHOT* for Java components).

   If the next release of Kurento only includes patches, then the next version number *6.9.1* is already good. However, maybe our release includes new functionality, which according to Semantic Versioning should be accompanied with a bump in the *minor* version number, so the next release version number should be *6.10.0*.

   To bump all versions to *6.10.0* run this:

   .. code-block:: bash

      cd kms-omni-build
      ./bin/set-versions.sh 6.10.0 --debian 0kurento1 \
          --release --commit --tag

   The result is that now all project versions are **6.10.0** and all Debian package versions will be **6.10.0-0kurento1**. All changes have been committed, and the tag ``6.10.0`` has been created.

   If you are repackaging an already released version (for example, because maybe after release you found out that the packages fail to install) then just increment the Debian package version: *0kurento2*.

#. Push the changes to all remote repositories.

   .. code-block:: bash

      git submodule foreach 'git push --follow-tags'

#. Start the `KMS CI job`_ with the parameters ``JOB_RELEASE`` **ENABLED** and ``JOB_ONLY_KMS`` **DISABLED**.

   If the release doesn't include any version of the

#. Wait until all packages get created and published correctly. Fix any issues that appear.

   The KMS CI job is a *Jenkins MultiJob Project*. If it fails at any stage, after fixing the cause of the error it's not needed to start the job again from the beginning; instead, it is possible to resume the build from the point it was before the failure. For this, just open the latest build number that failed (with a red marker in the *Build History* panel at the left of the job page); in the description of the build, the action *Resume build* is available on the left side.

#. Check that the Auto-Generated API Client JavaScript repos have been updated (which should happen as part of the CI jobs for all Kurento Media Server modules that contain API Definition files (``.KMD``):

   - kms-core -> kurento-client-core-js
   - kms-elements -> kurento-client-elements-js
   - kms-filters -> kurento-client-filters-js
   - kms-chroma -> kurento-module-chroma-js
   - kms-crowddetector -> kurento-module-crowddetector-js
   - kms-platedetector -> kurento-module-platedetector-js
   - kms-pointerdetector -> kurento-module-pointerdetector-js

#. When all repos have been released, and CI jobs have finished successfully:

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repository.
   - Inspect contents to ensure they are as expected:

     - kurento-module-creator (if it was released)
     - kms-api-core
     - kms-api-elements
     - kms-api-filters

   - **Close** repository.
   - Wait a bit.
   - **Refresh**.
   - **Release** repository.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

#. AFTER THE WHOLE RELEASE HAS BEEN COMPLETED: Set the next development version in all projects. Use the script ``kms-omni-build/bin/set-versions.sh`` to set version numbers, and commit.

   .. code-block:: bash

      cd kms-omni-build
      ./bin/set-versions.sh <NextVersion> --debian <DebianVersion> \
          --commit

   To choose the next version number, increment the **patch** number. For example, if the last release has been **6.10.0**, then the next development version number should be **6.10.1**:

   .. code-block:: bash

      cd kms-omni-build
      ./bin/set-versions.sh 6.10.1 --debian 0kurento1 \
          --commit

#. Push the changes to all remote repositories.

   .. code-block:: bash

      git submodule foreach 'git push'



Closing: kms-omni-build
-----------------------

As part of the release, update the submodule references of this repo, and create a tag just like in all the other repos:

.. code-block:: bash

   NEW_VERSION="6.9.0"
   COMMIT_MSG="Prepare release $NEW_VERSION"

   git add .
   git commit -m "$COMMIT_MSG"
   git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
   git push --follow-tags



Kurento JavaScript client
=========================

Release order:

- kurento-jsonrpc-js
- kurento-utils-js
- kurento-client-js
- kurento-tutorial-js
- kurento-tutorial-node

For each project above:

1. Prepare release.
2. Push a new tag to Git.
3. Move to next development version.



Release steps
-------------

#. Set the definitive release version in all projects. This operation varies between projects:

   - kurento-jsonrpc-js, kurento-utils-js, kurento-client-js: in file **package.json**.
   - kurento-tutorial-js: in each one of the **bower.json** files.
   - kurento-tutorial-node: in each one of the **package.json** files.

#. Review all dependencies to remove *-dev* versions.

   This command can be used to search for all *-dev* versions:

   .. code-block:: bash

      grep -Fr -- '-dev"'

#. Test the build.

   Use the profile '*kurento-release*' to enforce no *SNAPSHOT* dependencies are present.

   .. code-block:: bash

      PROJECTS=(
          kurento-jsonrpc-js
          kurento-utils-js
          kurento-client-js
      )

      for PROJECT in "${PROJECTS[@]}"; do
          pushd "$PROJECT"
          npm install
          node_modules/.bin/grunt
          node_modules/.bin/grunt sync:bower
          popd  # $PROJECT
      done

   If the beautifier step fails, use Grunt to run the beautifier and fix all files that need it:

   .. code-block:: bash

      npm install
      node_modules/.bin/grunt jsbeautifier::file:<FilePath>.js

   Some times it's needed to run Grunt a couple of times until it ends without errors.

#. **All-In-One** script:

   (Note: Always use ``mvn --batch-mode`` if you copy this to an actual script!)

   .. code-block:: bash

      NEW_VERSION="6.9.0"
      COMMIT_MSG="Prepare release $NEW_VERSION"

      PROJECTS=(
          kurento-jsonrpc-js
          kurento-utils-js
          kurento-client-js
          kurento-tutorial-js
          kurento-tutorial-node
      )

      for PROJECT in "${PROJECTS[@]}"; do
          pushd "$PROJECT"
          git pull --rebase
          for FILE in $(find . -name *.json); do
              TEMP="$(mktemp)"
              jq "if has(\"version\") then .version = \"$NEW_VERSION\" else . end" \
                  "$FILE" > "$TEMP" && mv "$TEMP" "$FILE"
              git add "$FILE"
          done
          git commit -m "$COMMIT_MSG"
          git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
          git push --follow-tags
          popd  # $PROJECT
      done

#. AFTER THE WHOLE RELEASE HAS BEEN COMPLETED: Set the next development version in all projects. To choose the next version number, increment the **patch** number and add "*-dev*".

   **All-In-One** script:

   .. code-block:: bash

      NEW_VERSION="6.9.1-dev"
      COMMIT_MSG="Prepare for next development iteration"

      PROJECTS=(
          kurento-jsonrpc-js
          kurento-utils-js
          kurento-client-js
          kurento-tutorial-js
          kurento-tutorial-node
      )

      for PROJECT in "${PROJECTS[@]}"; do
          pushd "$PROJECT"
          for FILE in $(find . -name *.json); do
              TEMP="$(mktemp)"
              jq "if has(\"version\") then .version = \"$NEW_VERSION\" else . end" \
                  "$FILE" > "$TEMP" && mv "$TEMP" "$FILE"
              git add "$FILE"
          done
          git commit -m "$COMMIT_MSG"
          git push
          popd  # $PROJECT
      done



Kurento Maven plugin
====================

1. Edit *pom.xml* to update the version field: remove "*-SNAPSHOT*".

   .. code-block:: xml

         <groupId>org.kurento</groupId>
         <artifactId>kurento-maven-plugin</artifactId>
      -  <version>1.2.3-SNAPSHOT</version>
      +  <version>1.2.3</version>
         <packaging>maven-plugin</packaging>

2. Edit *changelog* to add latest changes.

   Use this command to get a list of commit messages since last release:

   .. code-block:: bash

      git log "$(git describe --tags --abbrev=0)"..HEAD --oneline

3. Commit & push.

   .. code-block:: bash

      NEW_VERSION="1.2.3"
      COMMIT_MSG="Prepare release $NEW_VERSION"
      git add pom.xml changelog
      git commit -m "$COMMIT_MSG"
      git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
      git push --follow-tags

4. The release procedure should start automatically; some tests are run as a result of this commit, so you should wait for their completion.

5. Edit *pom.xml* to update the version field: increment the **patch** number and add "*-SNAPSHOT*".

   .. code-block:: xml

         <groupId>org.kurento</groupId>
         <artifactId>kurento-maven-plugin</artifactId>
      -  <version>1.2.3</version>
      +  <version>1.2.4-SNAPSHOT</version>
         <packaging>maven-plugin</packaging>

6. Commit & push.

   .. code-block:: bash

      COMMIT_MSG="Prepare for next development iteration"
      git add pom.xml
      git commit -m "$COMMIT_MSG"
      git push



Kurento Java client
===================

Release order:

- kurento-qa-pom
- kurento-java
- kurento-tutorial-java
- kurento-tutorial-test

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

#. Set the definitive release version in all projects. This operation varies between projects. Also, *kurento-tutorial-java* and *kurento-tutorial-test* require that *kurento-java* has been installed locally before being able to change their version numbers programmatically.

#. Review all dependencies to remove *SNAPSHOT* versions. In project *kurento-java*, all dependencies are defined as properties in the file *kurento-parent-pom/pom.xml*.

   This command can be used to search for all *SNAPSHOT* versions:

   .. code-block:: bash

      grep -Fr -- '-SNAPSHOT'

#. Test the build.

   Use the profile '*kurento-release*' to enforce no *SNAPSHOT* dependencies are present.

   .. code-block:: bash

      pushd kurento-qa-pom
      mvn -U clean install -Dmaven.test.skip=true -Pkurento-release
      popd  # kurento-qa-pom

      pushd kurento-java
      mvn -U clean install -Dmaven.test.skip=true -Pkurento-release
      popd  # kurento-java

      PROJECTS=(kurento-tutorial-java kurento-tutorial-test)
      for PROJECT in "${PROJECTS[@]}"; do
          pushd "$PROJECT"
          mvn -U clean install -Dmaven.test.skip=true -Pkurento-release
          popd  # $PROJECT
      done

#. (Only *kurento-java*) If the build works, install locally. This will be needed to later update the version of *kurento-tutorial-java* and *kurento-tutorial-test*.

#. **All-In-One** script:

   (Note: Always use ``mvn --batch-mode`` if you copy this to an actual script!)

   .. code-block:: bash

      NEW_VERSION="6.9.0"
      COMMIT_MSG="Prepare release $NEW_VERSION"

      pushd kurento-qa-pom
      git pull --rebase
      mvn versions:set -DgenerateBackupPoms=false \
          -DnewVersion="$NEW_VERSION"
      git ls-files --modified | grep 'pom.xml' | xargs -r git add
      git commit -m "$COMMIT_MSG"
      git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
      git push --follow-tags
      popd  # kurento-qa-pom

      pushd kurento-java
      git pull --rebase
      mvn versions:set -DgenerateBackupPoms=false \
          -DnewVersion="$NEW_VERSION" \
          --file kurento-parent-pom/pom.xml
      mvn -U clean install -Dmaven.test.skip=true \
          -Pkurento-release
      git clean -xdf  # Delete build files
      git ls-files --modified | grep 'pom.xml' | xargs -r git add
      git commit -m "$COMMIT_MSG"
      git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
      git push --follow-tags
      popd  # kurento-java

      PROJECTS=(kurento-tutorial-java kurento-tutorial-test)
      for PROJECT in "${PROJECTS[@]}"; do
          pushd "$PROJECT"
          git pull --rebase
          mvn versions:update-parent -DgenerateBackupPoms=false \
              -DallowSnapshots=false \
              -DparentVersion="[${NEW_VERSION}]"
          mvn -N versions:update-child-modules -DgenerateBackupPoms=false \
              -DallowSnapshots=false
          git clean -xdf  # Delete build files
          git ls-files --modified | grep 'pom.xml' | xargs -r git add
          git commit -m "$COMMIT_MSG"
          git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
          git push --follow-tags
          popd  # $PROJECT
      done

#. When all repos have been released, and CI jobs have finished successfully:

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repositories.
   - Inspect contents to ensure they are as expected: *kurento-java*, etc.
   - **Close repositories**.
   - Wait a bit.
   - **Refresh**.
   - **Release repositories**.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

   - Open the `Nexus Sonatype Staging Repositories`_ section.
   - Select **kurento** repository.
   - Inspect contents to ensure they are as expected:

     - kurento-client
     - kurento-commons
     - kurento-integration-tests
     - kurento-java
     - kurento-jsonrpc
     - kurento-jsonrpc-client
     - kurento-jsonrpc-client-jetty
     - kurento-jsonrpc-server
     - kurento-parent-pom
     - kurento-repository (ABANDONED PROJECT)
     - kurento-repository-client (ABANDONED)
     - kurento-repository-internal (ABANDONED)
     - kurento-test

   - **Close** repository.
   - Wait a bit.
   - **Refresh**.
   - **Release** repository.
   - Maven artifacts will be available `after 10 minutes <https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central>`__.

#. AFTER THE WHOLE RELEASE HAS BEEN COMPLETED: Set the next development version in all projects. To choose the next version number, increment the **patch** number and add "*-SNAPSHOT*". Maven can do this automatically with the `Maven Versions Plugin`_.

   **All-In-One** script:

   (Note: Always use ``mvn --batch-mode`` if you copy this to an actual script!)

   .. code-block:: bash

      COMMIT_MSG="Prepare for next development iteration"

      pushd kurento-qa-pom
      mvn versions:set -DgenerateBackupPoms=false \
          -DnextSnapshot=true
      git ls-files --modified | grep 'pom.xml' | xargs -r git add
      git commit -m "$COMMIT_MSG"
      git push
      popd  # kurento-qa-pom

      pushd kurento-java
      mvn versions:set -DgenerateBackupPoms=false \
          -DnextSnapshot=true \
          --file kurento-parent-pom/pom.xml
      mvn -U clean install -Dmaven.test.skip=true
      git clean -xdf  # Delete build files
      git ls-files --modified | grep 'pom.xml' | xargs -r git add
      git commit -m "$COMMIT_MSG"
      git push
      popd  # kurento-java

      PROJECTS=(kurento-tutorial-java kurento-tutorial-test)
      for PROJECT in "${PROJECTS[@]}"; do
          pushd "$PROJECT"
          mvn versions:update-parent -DgenerateBackupPoms=false \
              -DallowSnapshots=true
          mvn -N versions:update-child-modules -DgenerateBackupPoms=false \
              -DallowSnapshots=true
          git clean -xdf  # Delete build files
          git ls-files --modified | grep 'pom.xml' | xargs -r git add
          git commit -m "$COMMIT_MSG"
          git push
          popd  # $PROJECT
      done



Docker images
=============

A new set of development images is deployed to `Kurento Docker Hub`_ on each nightly build. Besides, a release version will be published as part of the CI jobs chain when the `KMS CI job`_ is triggered.



Kurento documentation
=====================

The documentation scripts will download both Java and JavaScript clients, generate HTML Javadoc / Jsdoc pages from them, and embed everything into a `static section <https://doc-kurento.readthedocs.io/en/stable/features/kurento_client.html#reference-documentation>`__.

For this reason, the documentation must be built only after all the other modules have been released.

#. Ensure that the whole nightly CI chain works:

   Job *doc-kurento* -> job *doc-kurento-readthedocs* -> `New build at ReadTheDocs <https://readthedocs.org/projects/doc-kurento/builds/>`__.

#. Edit `VERSIONS.conf.sh`_ to set all relevant version numbers: version of the documentation itself, and all referred modules and client libraries. These numbers can be different because not all of the Kurento projects are necessarily released with the same frequency.

#. Test the build locally, check everything works:

   .. code-block:: bash

      make html

#. Git add, commit, tag, push:

   .. code-block:: bash

      NEW_VERSION="6.9.0"
      COMMIT_MSG="Prepare release $NEW_VERSION"

      git add VERSIONS.conf.sh
      git commit -m "$COMMIT_MSG"
      git tag -a -m "$COMMIT_MSG" "$NEW_VERSION"
      git push --follow-tags

#. Run the `doc-kurento CI job`_ with the parameter ``JOB_RELEASE`` **ENABLED**.

#. CI automatically tags Release versions in `doc-kurento <https://github.com/Kurento/doc-kurento/releases>`__ and in `doc-kurento-readthedocs <https://github.com/Kurento/doc-kurento-readthedocs/releases>`__, so the release will show up as "*stable*" in ReadTheDocs.

#. Open the `ReadTheDocs Versions dashboard <https://readthedocs.org/dashboard/doc-kurento/versions/>`__ and in the *Default Version* Combo Box select the latest version available.

   This field is not set to "*stable*" because we want that the actual version number gets shown in the upper part of the side panel (below the Kurento logo, above the search box) when users open the documentation. If "*stable*" was selected here, then users would just see the word "*stable*" in the mentioned panel.



.. Kurento links

.. _Kurento Docker Hub: https://hub.docker.com/u/kurento/
.. _KMS CI job: https://ci.openvidu.io/jenkins/job/Development/job/00_KMS_BUILD_ALL/
.. _doc-kurento CI job: https://ci.openvidu.io/jenkins/job/Development/job/kurento_doc_merged/
.. _VERSIONS.conf.sh: https://github.com/Kurento/doc-kurento/blob/e021a6c98bcea4db351faf423e90b64b8aa977f6/VERSIONS.conf.sh



.. External links

.. _Debian Policy Manual: https://www.debian.org/doc/debian-policy/ch-controlfields.html#version
.. _Maven Versions Plugin: https://www.mojohaus.org/versions-maven-plugin/set-mojo.html#nextSnapshot
.. _Nexus Sonatype Staging Repositories: https://oss.sonatype.org/#stagingRepositories
.. _Semantic Versioning: https://semver.org/spec/v2.0.0.html#summary
.. _this Ask Ubuntu answer: https://askubuntu.com/questions/620533/what-is-the-meaning-of-the-xubuntuy-string-in-ubuntu-package-names/620539#620539
