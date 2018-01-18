# Makefile for Sphinx documentation

VERSION := $(shell cat VERSION)

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
SPHINXPROJ    = Kurento
SOURCEDIR     = source
BUILDDIR      = build
WORKDIR       = $(BUILDDIR)/$(SOURCEDIR)

.NOTPARALLEL:
.ONESHELL:
.PHONY: help init-workdir Makefile*

# Put it first so that "make" without argument is like "make help".
help:
	@$(SPHINXBUILD) -M $@ "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
	@echo "  langdoc     to make JavaDocs and JsDocs of the Kurento Clients"
	@echo "  dist        to make <langdoc html epub latexpdf> and then pack"
	@echo "              all resulting files as kurento-doc-$(VERSION).tgz"
	@echo "  readthedocs to make <langdoc> and then copy the results to the"
	@echo "              Sphinx theme's static folder"
	@echo ""
	@echo "apt-get dependencies:"
	@echo "- javadoc (java-sdk-headless)"
	@echo "- npm"
	@echo "- latexmk"
	@echo "- texlive-fonts-recommended"
	@echo "- texlive-latex-recommended"
	@echo "- texlive-latex-extra"
	@echo ""
	@echo "python pip dependencies:"
	@echo "- sphinx >= 1.5.0 (Tested: 1.6.6)"
	@echo "- sphinx_rtd_theme"

init-workdir:
	mkdir -p $(WORKDIR)
	rsync -a $(SOURCEDIR)/ $(WORKDIR)
	rsync -a VERSION $(WORKDIR)
	# Replace all instances of '|VERSION|' with the appropriate value
	grep -rlZ "|VERSION|" $(WORKDIR) | xargs -0rL1 sed -i -e "s/|VERSION|/$(VERSION)/g"

langdoc:
	# Care must be taken because the Current Directory changes in this target,
	# so it's better to use absolute paths for destination dirs.
	# The 'client-doc' part must match the setting 'html_static_path' in 'conf.py',
	# and its contents must match the URLs used in the documentation files.
	$(eval WORKPATH := $(CURDIR)/$(BUILDDIR)/client-src)
	$(eval DESTPATH := $(CURDIR)/$(BUILDDIR)/langdoc)
	mkdir -p $(WORKPATH)
	mkdir -p $(DESTPATH)

	# kurento-client javadoc
	cd $(WORKPATH)
	git clone https://github.com/Kurento/kurento-java.git \
		&& cd kurento-java
	git checkout $(VERSION) || echo "Using master branch"
	cd kurento-client || { echo "ERROR: 'cd' failed. ls:"; ls; exit 1; }
	mvn clean package -DskipTests || { echo "ERROR: 'mvn clean' failed"; exit 1; }
	mvn javadoc:javadoc -DreportOutputDirectory="$(DESTPATH)" -DdestDir="client-javadoc" \
		-Dsourcepath="src/main/java:target/generated-sources/kmd" \
		-Dsubpackages="org.kurento.client" -DexcludePackageNames="*.internal" \
		|| { echo "ERROR: 'mvn javadoc' failed"; exit 1; }

	# kurento-client-js jsdoc
	cd $(WORKPATH)
	git clone https://github.com/Kurento/kurento-client-js.git \
		&& cd kurento-client-js
	git checkout $(VERSION) || echo "Using master branch"
	npm install
	node_modules/.bin/grunt --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/ $(DESTPATH)/client-jsdoc

	# kurento-utils-js jsdoc
	cd $(WORKPATH)
	git clone https://github.com/Kurento/kurento-utils-js.git \
		&& cd kurento-utils-js
	git checkout $(VERSION) || echo "Using master branch"
	npm install
	node_modules/.bin/grunt --force jsdoc \
		|| { echo "ERROR: 'grunt jsdoc' failed"; exit 1; }
	rsync -a doc/jsdoc/kurento-utils/*/ $(DESTPATH)/utils-jsdoc

dist: langdoc html epub latexpdf
	$(eval DISTDIR := $(BUILDDIR)/dist/kurento-doc-$(VERSION))
	mkdir -p $(DISTDIR)
	rsync -a $(BUILDDIR)/html $(BUILDDIR)/epub/Kurento.epub \
		$(BUILDDIR)/latex/Kurento.pdf $(DISTDIR)
	tar zcf $(DISTDIR).tgz -C $(DISTDIR) .

# Target to be run by CI. It modifies the source directory,
# so the worspace should get deleted afterwards.
ci-readthedocs: init-workdir langdoc
	rsync -a $(WORKDIR)/ $(SOURCEDIR)
	rsync -a $(BUILDDIR)/langdoc $(SOURCEDIR)

# Comment this target to disable generation of JavaDoc & JsDoc
#html: langdoc

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option. $(O) is meant as a shortcut for $(SPHINXOPTS).
%: init-workdir Makefile*
	$(SPHINXBUILD) -M $@ "$(WORKDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
