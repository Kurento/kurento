# Makefile for Sphinx documentation

# Special Make configuration:
# - Run all targets sequentially (disable parallel jobs)
#   See: https://www.gnu.org/software/make/manual/html_node/Parallel.html
# - Run all commands in the same shell (disable one shell per command)
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
SPHINXOPTS  := -v
SPHINXBUILD := sphinx-build
SPHINXPROJ  := Kurento
SOURCEDIR   := source
BUILDDIR    := build
WORKDIR     := $(CURDIR)/$(BUILDDIR)/$(SOURCEDIR)

# Put this target first so that "make" without argument is like "make help"
help:
	@$(SPHINXBUILD) -M $@ "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
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
	mkdir -p $(WORKDIR)
	rsync -a $(SOURCEDIR)/ $(WORKDIR)

langdoc-init:
	# Care must be taken because the Current Directory changes in this target,
	# so it's better to use absolute paths for destination dirs.
	# DESTPATH must be added to the setting 'html_static_path' in 'conf.py',
	# and its contents must match the URLs used in the documentation files.
	$(eval WORKPATH := $(CURDIR)/$(BUILDDIR)/client-src)
	$(eval DESTPATH := $(CURDIR)/$(BUILDDIR)/langdoc)
	mkdir -p $(WORKPATH)
	mkdir -p $(DESTPATH)

langdoc-client-java: langdoc-init
	cd $(WORKPATH)
	[ -d kurento ] || git clone https://github.com/Kurento/kurento.git
	cd kurento/clients/java/
	[ "|VERSION_RELEASE|" = "true" ] && git checkout "|VERSION_CLIENT_JAVA|"
	cd kurento-client || { echo "ERROR: 'cd' failed, ls:"; ls -lA; exit 1; }
	mvn --batch-mode --quiet -Psnapshot -DskipTests=true clean package \
		|| { echo "ERROR: 'mvn clean package' failed"; exit 1; }
	mvn --batch-mode --quiet javadoc:javadoc \
		-DreportOutputDirectory="$(DESTPATH)" \
		-DdestDir="client-javadoc" \
		-Dsourcepath="src/main/java;target/generated-sources/kmd" \
		-Dsubpackages="org.kurento.client" \
		-DexcludePackageNames="*.internal" \
		|| { echo "ERROR: 'mvn javadoc' failed"; exit 1; }

langdoc-client-js: langdoc-init
	cd $(WORKPATH)
	[ -d kurento ] || git clone https://github.com/Kurento/kurento.git
	cd kurento/clients/javascript/client/
	[ "|VERSION_RELEASE|" = "true" ] && git checkout "|VERSION_CLIENT_JS|"
	npm install --no-color
	node_modules/.bin/grunt --no-color --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/ $(DESTPATH)/client-jsdoc

langdoc-utils-js: langdoc-init
	cd $(WORKPATH)
	[ -d kurento ] || git clone https://github.com/Kurento/kurento.git
	cd kurento/browser/kurento-utils-js/
	[ "|VERSION_RELEASE|" = "true" ] && git checkout "|VERSION_UTILS_JS|"
	npm install --no-color
	node_modules/.bin/grunt --no-color --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/kurento-utils/*/ $(DESTPATH)/utils-jsdoc

langdoc: langdoc-client-java langdoc-client-js langdoc-utils-js

dist: langdoc html epub latexpdf
	$(eval DISTDIR := $(BUILDDIR)/dist/kurento-doc-|VERSION_DOC|)
	mkdir -p $(DISTDIR)
	rsync -a $(BUILDDIR)/html $(BUILDDIR)/epub/Kurento.epub \
		$(BUILDDIR)/latex/Kurento.pdf $(DISTDIR)
	tar zcf $(DISTDIR).tgz -C $(DISTDIR) .

# Target to be run by CI. It modifies the source directory,
# so the worspace should get deleted afterwards.
ci-readthedocs: init-workdir langdoc
	rsync -a $(WORKDIR)/ $(SOURCEDIR)
	rsync -a $(BUILDDIR)/langdoc $(SOURCEDIR)

# Comment this target to disable unconditional generation of JavaDoc & JsDoc
#html: langdoc

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option. $(O) is meant as a shortcut for $(SPHINXOPTS).
%: init-workdir Makefile*
	$(SPHINXBUILD) -M $@ "$(WORKDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
