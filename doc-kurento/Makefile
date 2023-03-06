# Makefile for Sphinx documentation

# Special Make configuration:
# * Run all targets sequentially (disable parallel jobs)
#   See: https://www.gnu.org/software/make/manual/html_node/Parallel.html
# * Run all commands in the same shell (disable one shell per command)
#   See: https://www.gnu.org/software/make/manual/html_node/One-Shell.html
.NOTPARALLEL:
.ONESHELL:
.PHONY: help init-workdir langdoc Makefile*

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
MAVEN_ARGS := --batch-mode --quiet

BUILD_DIR := build
SOURCE_DIR := source
SPHINX_WORKDIR := $(CURDIR)/$(BUILD_DIR)/$(SOURCE_DIR)

# Fully-qualified plugin names, to use newer versions than the Maven defaults.
MAVEN_JAVADOC_PLUGIN := org.apache.maven.plugins:maven-javadoc-plugin:3.5.0

# Put this target first so that "make" without argument is like "make help"
help:
	@$(SPHINXBUILD) -M $@ "$(SOURCE_DIR)" "$(BUILD_DIR)" $(SPHINXOPTS) $(O)
	@echo "  langdoc     to make JavaDocs and JsDocs of the Kurento Clients"
	@echo "  dist        to make <langdoc html epub latexpdf> and then pack"
	@echo "              all resulting files as kurento-doc-|VERSION_DOC|.tgz"
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

init-workdir:
	mkdir -p $(SPHINX_WORKDIR)
	rsync -a $(SOURCE_DIR)/ $(SPHINX_WORKDIR)

langdoc-init:
	# DESTPATH must be added to the setting 'html_static_path' in 'conf.py',
	# and its contents must match the URLs used in the documentation files.
	$(eval DESTPATH := $(CURDIR)/$(BUILD_DIR)/langdoc)
	mkdir -p $(DESTPATH)

langdoc-client-java: langdoc-init
	pushd ../clients/java/client/ || { echo "ERROR: 'cd' failed"; exit 1; }
	$(MAVEN) $(MAVEN_ARGS) -Psnapshot -DskipTests=true clean package \
		|| { echo "ERROR: '$(MAVEN) clean package' failed"; exit 1; }
	$(MAVEN) $(MAVEN_ARGS) $(MAVEN_JAVADOC_PLUGIN):javadoc \
		-DreportOutputDirectory="$(DESTPATH)" \
		-DdestDir="client-javadoc" \
		-Dsourcepath="src/main/java;target/generated-sources/kmd" \
		-Dsubpackages="org.kurento.client" \
		-DexcludePackageNames="*.internal" \
		|| { echo "ERROR: '$(MAVEN) javadoc' failed"; exit 1; }
	popd

langdoc-client-js: langdoc-init
	pushd ../clients/javascript/client/
	npm install --no-color
	node_modules/.bin/grunt --no-color --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/ $(DESTPATH)/client-jsdoc
	popd

langdoc-utils-js: langdoc-init
	pushd ../browser/kurento-utils-js/
	npm install --no-color
	node_modules/.bin/grunt --no-color --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/kurento-utils/*/ $(DESTPATH)/utils-jsdoc
	popd

langdoc: langdoc-client-java langdoc-client-js langdoc-utils-js

dist: langdoc html epub latexpdf
	$(eval DISTDIR := $(BUILD_DIR)/dist/kurento-doc-|VERSION_DOC|)
	mkdir -p $(DISTDIR)
	rsync -a $(BUILD_DIR)/html $(BUILD_DIR)/epub/Kurento.epub \
		$(BUILD_DIR)/latex/Kurento.pdf $(DISTDIR)
	tar zcf $(DISTDIR).tgz -C $(DISTDIR) .

# Target to be run by CI.
# It modifies the source directory, adding the langdoc results, so the worspace
# should get cleaned up or deleted afterwards by the CI job.
ci-readthedocs: init-workdir langdoc
	rsync -a $(SPHINX_WORKDIR)/ $(SOURCE_DIR)
	rsync -a $(BUILD_DIR)/langdoc $(SOURCE_DIR)

# Comment this target to disable unconditional generation of JavaDoc & JsDoc
#html: langdoc

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option. $(O) is meant as a shortcut for $(SPHINXOPTS).
%: init-workdir Makefile*
	$(SPHINXBUILD) -M $@ "$(SPHINX_WORKDIR)" "$(BUILD_DIR)" $(SPHINXOPTS) $(O)
