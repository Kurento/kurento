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
.PHONY: Makefile help substitutions

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

# Replace all instances of '|VERSION|' with the appropriate value
substitutions:
	mkdir -p $(WORKDIR)
	rsync -a $(SOURCEDIR)/ $(WORKDIR)
	rsync -a VERSION $(WORKDIR)
	grep -rlZ "|VERSION|" $(WORKDIR) | xargs -0rL1 sed -i -e "s/|VERSION|/$(VERSION)/g"

langdoc:
	# Care must be taken because the Current Directory changes in this target,
	# so it's better to use absolute paths for destination dirs.
	$(eval WORKPATH    := $(CURDIR)/$(BUILDDIR)/langdoc-src)

	# The 'langdoc' part must match the setting 'html_static_path' in 'conf.py',
	# and the last part must match the URLs used in the documentation files.
	$(eval JAVADOCPATH := $(CURDIR)/$(BUILDDIR)/langdoc/javadoc)
	$(eval JSDOCPATH   := $(CURDIR)/$(BUILDDIR)/langdoc/jsdoc)

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

# Target to be run by CI. It modifies the source directory,
# so the worspace should get deleted afterwards.
ci-readthedocs: langdoc substitutions
	rsync -a $(WORKDIR)/ $(SOURCEDIR)
	rsync -a $(BUILDDIR)/langdoc $(SOURCEDIR)

# Comment this target to disable generation of JavaDoc & JsDoc
#html: langdoc

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option. $(O) is meant as a shortcut for $(SPHINXOPTS).
%: Makefile substitutions
	$(SPHINXBUILD) -M $@ "$(WORKDIR)" "$(BUILDDIR)" $(SPHINXOPTS) $(O)
