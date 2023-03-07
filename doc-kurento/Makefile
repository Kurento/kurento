# Makefile for Sphinx documentation

# Run all targets sequentially (disable parallel jobs)
# See: https://www.gnu.org/software/make/manual/html_node/Parallel.html
.NOTPARALLEL:

# Run all commands in the same shell (disable one shell per command)
# See: https://www.gnu.org/software/make/manual/html_node/One-Shell.html
.ONESHELL:

.PHONY: help init langdoc Makefile*

# Use Bash shell for commands.
SHELL := /bin/bash

# Check required features
ifeq ($(filter oneshell,$(.FEATURES)),)
$(error This Make doesn't support '.ONESHELL', use Make >= 3.82)
endif

# You can set these variables from the command line.
# Adding more '-v' increases the log verbosity level.
SPHINXBUILD := sphinx-build
SPHINXOPTS := -v
SPHINXPROJ := Kurento

MAVEN := mvn
MAVEN_ARGS := --batch-mode --settings /maven-settings.xml

BUILD := build
DIST := dist
DIST_NAME := kurento-doc
LANGDOC := langdoc
SOURCE := source

DIST_PATH := $(CURDIR)/$(DIST)/$(DIST_NAME)
LANGDOC_PATH := $(CURDIR)/$(BUILD)/$(LANGDOC)
SPHINX_SRC_PATH := $(CURDIR)/$(BUILD)/$(SOURCE)

# Fully-qualified plugin names, to use newer versions than the Maven defaults.
MAVEN_JAVADOC_PLUGIN := org.apache.maven.plugins:maven-javadoc-plugin:3.5.0

# Put this target first so that "make" without argument is like "make help"
help:
	@$(SPHINXBUILD) -M $@ "$(SOURCE)" "$(BUILD)" $(SPHINXOPTS) $(O)
	@echo "  langdoc     to make JavaDoc and JsDoc of the Kurento Clients"
	@echo "  dist        to make <langdoc html epub latexpdf> and then pack"
	@echo "              all resulting files as $(DIST_NAME).tgz"
	@echo "  readthedocs to make <langdoc> and then copy the results to the"
	@echo "              Sphinx theme's static folder"
	@echo ""
	@echo "apt-get dependencies:"
	@echo "- make >= 3.82"
	@echo "- javadoc (default-jdk-headless)"
	@echo "- npm"
	@echo "- latexmk"
	@echo "- texlive-fonts-recommended"
	@echo "- texlive-latex-recommended"
	@echo "- texlive-latex-extra"

init:
	mkdir -p \
		$(DIST_PATH) \
		$(LANGDOC_PATH) \
		$(SPHINX_SRC_PATH)
	rsync -a $(SOURCE)/ $(SPHINX_SRC_PATH)/
	./configure.sh --source $(SPHINX_SRC_PATH)

langdoc-client-java:
	pushd ../clients/java/client/ || { echo "ERROR: 'cd' failed"; exit 1; }
	$(MAVEN) $(MAVEN_ARGS) -Psnapshot -DskipTests=true clean package \
		|| { echo "ERROR: '$(MAVEN) clean package' failed"; exit 1; }
	$(MAVEN) $(MAVEN_ARGS) $(MAVEN_JAVADOC_PLUGIN):javadoc \
		-DreportOutputDirectory="$(LANGDOC_PATH)" \
		-DdestDir="client-javadoc" \
		-Dsourcepath="src/main/java;target/generated-sources/kmd" \
		-Dsubpackages="org.kurento.client" \
		-DexcludePackageNames="*.internal" \
		|| { echo "ERROR: '$(MAVEN) javadoc' failed"; exit 1; }
	popd

langdoc-client-js:
	pushd ../clients/javascript/client/ || { echo "ERROR: 'cd' failed"; exit 1; }
	npm install
	node_modules/.bin/grunt --no-color --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/ $(LANGDOC_PATH)/client-jsdoc/
	popd

langdoc-utils-js:
	pushd ../browser/kurento-utils-js/ || { echo "ERROR: 'cd' failed"; exit 1; }
	npm install
	node_modules/.bin/grunt --no-color --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/kurento-utils/*/ $(LANGDOC_PATH)/utils-jsdoc/
	popd

langdoc: init langdoc-client-java langdoc-client-js langdoc-utils-js

dist: langdoc html epub latexpdf
	rsync -a \
		$(BUILD)/html \
		$(BUILD)/epub/Kurento.epub \
		$(BUILD)/latex/Kurento.pdf \
		$(DIST_PATH)/
	tar zcf $(DIST_PATH).tgz -C $(DIST_PATH) .

# Target to be run by CI.
# It modifies the source directory, adding the langdoc results, so the worspace
# should get cleaned up or deleted afterwards by the CI job.
ci-readthedocs: langdoc
	rsync -a $(SPHINX_SRC_PATH)/ $(SOURCE)/
	rsync -a $(BUILD)/langdoc $(SOURCE)/

# Comment this target to disable unconditional generation of JavaDoc & JsDoc
#html: langdoc

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option. $(O) is meant as a shortcut for $(SPHINXOPTS).
%: init Makefile*
	$(SPHINXBUILD) -M $@ "$(SPHINX_SRC_PATH)" "$(BUILD)" $(SPHINXOPTS) $(O)
