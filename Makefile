# Makefile for Sphinx documentation

VERSION := $(shell cat VERSION)

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
SPHINXPROJ    = Kurento
SOURCEDIR     = source
BUILDDIR      = build

.NOTPARALLEL:
.ONESHELL:
.PHONY: help Makefile

# Put it first so that "make" without argument is like "make help".
help:
	@$(SPHINXBUILD) -M $@ "$(SOURCEDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
	@echo "  langdoc     to make JavaDocs and JsDocs of the Kurento Clients"
	@echo "  dist        to make <langdoc html epub latexpdf> and then pack"
	@echo "              all resulting files as kurento-doc-$(VERSION).tgz"
	@echo "  readthedocs to make <langdoc> and then copy the results to the"
	@echo "              Sphinx theme's static folder"
	@echo ""
	@echo "Dependencies:"
	@echo "- javadoc (java-sdk-headless)"
	@echo "- npm"
	@echo "- python-sphinx"
	@echo "- python-sphinx-rtd-theme"
	@echo "- latexmk"
	@echo "- texlive-fonts-recommended"
	@echo "- texlive-latex-recommended"
	@echo "- texlive-latex-extra"

langdoc:
	# Care must be taken because the Current Directory changes in this target,
	# so it's better to use absolute paths for destination dirs.
	$(eval WORKPATH    := $(CURDIR)/$(BUILDDIR)/langdoc)
	$(eval JAVADOCPATH := $(CURDIR)/$(BUILDDIR)/html/features/javadoc)
	$(eval JSDOCPATH   := $(CURDIR)/$(BUILDDIR)/html/features/jsdoc)

	mkdir -p $(WORKPATH)
	mkdir -p $(JAVADOCPATH)
	mkdir -p $(JSDOCPATH)

	# kurento-client javadoc
	cd $(WORKPATH)
	git clone https://github.com/Kurento/kurento-java.git
	cd kurento-java
	git checkout kurento-java-$(VERSION) \
		|| git checkout $(VERSION) \
		|| echo "Using master branch"
	cd kurento-client
	mvn clean package -DskipTests || { echo "ERROR: Maven failed"; exit 1; }
	#J
	rsync -a target/generated-sources/kmd/* src/main/java
	javadoc -d $(JAVADOCPATH) -sourcepath src/main/java org.kurento.client
	#
	# mvn javadoc:javadoc -DdestDir="$(JAVADOCPATH)" \
	# 	-Dsourcepath="src/main/java:target/generated-sources/kmd" \
	# 	-Dsubpackages="org.kurento.client" -DexcludePackageNames="*.internal"

	# kurento-client-js jsdoc
	cd $(WORKPATH)
	git clone https://github.com/Kurento/kurento-client-js.git
	cd kurento-client-js
	git checkout kurento-client-js-$(VERSION) \
		|| git checkout $(VERSION) \
		|| echo "Using master branch"
	npm install
	node_modules/.bin/grunt --force jsdoc
	rsync -a doc/jsdoc/ $(JSDOCPATH)/kurento-client-js

	# kurento-utils-js jsdoc
	cd $(WORKPATH)
	git clone https://github.com/Kurento/kurento-utils-js.git
	cd kurento-utils-js
	git checkout kurento-utils-js-$(VERSION) \
		|| git checkout $(VERSION) \
		|| echo "Using master branch"
	npm install
	node_modules/.bin/grunt --force jsdoc
	rsync -a doc/jsdoc/kurento-utils/*/ $(JSDOCPATH)/kurento-utils-js

dist: langdoc html epub latexpdf
	$(eval DISTDIR := $(BUILDDIR)/dist/kurento-doc-$(VERSION))
	mkdir -p $(DISTDIR)
	rsync -a $(BUILDDIR)/html $(BUILDDIR)/epub/Kurento.epub \
		$(BUILDDIR)/latex/Kurento.pdf $(DISTDIR)
	tar zcf $(DISTDIR).tgz -C $(DISTDIR) .

# readthedocs: langdoc
	#J TODO REVIEW grep -rlZ "langdoc/" $(SOURCEDIR) | xargs -0 -L1 sed -i -e "s|langdoc/|_static/langdoc/|g"
	# rsync -a $(BUILDDIR)/html/langdoc $(SOURCEDIR)/themes/sphinx_rtd_theme/static

# Comment this target to disable generation of JavaDoc & JsDoc
html: langdoc

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option. $(O) is meant as a shortcut for $(SPHINXOPTS).
%: Makefile
	$(eval WORKDIR := $(BUILDDIR)/$(SOURCEDIR))
	mkdir -p $(WORKDIR)
	rsync -a $(SOURCEDIR)/ $(WORKDIR)
	rsync -a VERSION $(WORKDIR)
	grep -rlZ "|VERSION|" $(WORKDIR) | xargs -0 -L1 sed -i -e "s/|VERSION|/$(VERSION)/g"
	$(SPHINXBUILD) -M $@ "$(WORKDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
