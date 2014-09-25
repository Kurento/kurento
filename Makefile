# Makefile for Sphinx documentation
#

# Versions
DOC_VERSION = 5.0.4-dev
CLIENT_JAVA_VERSION = 
CLIENT_JS_VERSION = 
UTILS_JS_VERSION = 

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
PAPER         =
BUILDDIR      = build

# Internal variables.
PAPEROPT_a4     = -D latex_paper_size=a4
PAPEROPT_letter = -D latex_paper_size=letter
ALLSPHINXOPTS   = -d $(BUILDDIR)/doctrees $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) source
# the i18n builder cannot share the environment and doctrees with the others
I18NSPHINXOPTS  = $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) source

.PHONY: help clean html dirhtml singlehtml pickle json htmlhelp langdoc qthelp devhelp epub latex latexpdf text man changes linkcheck doctest gettext dist

help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  langdoc    to make javadocs and jsdocs of the Kurento Clients"
	@echo "  html       to make standalone HTML files"
	@echo "  dist       to make langdoc html epub latexpdf and then copy"
	@echo "             Kurento.{pdf,epub} in build/html and make a tgz"
	@echo "             as kurento-docs-$(DOC_VERSION).tgz"
	@echo "  dirhtml    to make HTML files named index.html in directories"
	@echo "  singlehtml to make a single large HTML file"
	@echo "  pickle     to make pickle files"
	@echo "  json       to make JSON files"
	@echo "  htmlhelp   to make HTML files and a HTML help project"
	@echo "  qthelp     to make HTML files and a qthelp project"
	@echo "  devhelp    to make HTML files and a Devhelp project"
	@echo "  epub       to make an epub"
	@echo "  latex      to make LaTeX files, you can set PAPER=a4 or PAPER=letter"
	@echo "  latexpdf   to make LaTeX files and run them through pdflatex"
	@echo "  text       to make text files"
	@echo "  man        to make manual pages"
	@echo "  texinfo    to make Texinfo files"
	@echo "  info       to make Texinfo files and run them through makeinfo"
	@echo "  gettext    to make PO message catalogs"
	@echo "  changes    to make an overview of all changed/added/deprecated items"
	@echo "  linkcheck  to check all external links for integrity"
	@echo "  doctest    to run all doctests embedded in the documentation (if enabled)"

clean:
	-rm -rf $(BUILDDIR)/*
	for p in $(APIS); do rm -rf source/$$p/com; done
	-rm -rf source/langdocs

html:
	$(SPHINXBUILD) -b html $(ALLSPHINXOPTS) $(BUILDDIR)/html
	find build/html -name "*.html" -exec sed -i -e "s@|DOC_VERSION|@$(DOC_VERSION)@" {} \;
	find build/html -name "*.html" -exec sed -i -e "s@|CLIENT_JAVA_VERSION|@$(CLIENT_JAVA_VERSION)@" {} \;
	find build/html -name "*.html" -exec sed -i -e "s@|CLIENT_JS_VERSION|@$(CLIENT_JS_VERSION)@" {} \;
	find build/html -name "*.html" -exec sed -i -e "s@|UTILS_JS_VERSION|@$(UTILS_JS_VERSION)@" {} \;
	./fixlinks.sh
	@echo
	@echo "Build finished. The HTML pages are in $(BUILDDIR)/html."

dirhtml:
	$(SPHINXBUILD) -b dirhtml $(ALLSPHINXOPTS) $(BUILDDIR)/dirhtml
	@echo
	@echo "Build finished. The HTML pages are in $(BUILDDIR)/dirhtml."

singlehtml:
	$(SPHINXBUILD) -b singlehtml $(ALLSPHINXOPTS) $(BUILDDIR)/singlehtml
	@echo
	@echo "Build finished. The HTML page is in $(BUILDDIR)/singlehtml."

pickle:
	$(SPHINXBUILD) -b pickle $(ALLSPHINXOPTS) $(BUILDDIR)/pickle
	@echo
	@echo "Build finished; now you can process the pickle files."

json:
	$(SPHINXBUILD) -b json $(ALLSPHINXOPTS) $(BUILDDIR)/json
	@echo
	@echo "Build finished; now you can process the JSON files."

htmlhelp:
	$(SPHINXBUILD) -b htmlhelp $(ALLSPHINXOPTS) $(BUILDDIR)/htmlhelp
	@echo
	@echo "Build finished; now you can run HTML Help Workshop with the" \
	      ".hhp project file in $(BUILDDIR)/htmlhelp."

langdoc:
	  mkdir -p $(BUILDDIR)/langdoc
	  rm -rf $(BUILDDIR)/langdoc/kurento-java
	  mkdir -p $(BUILDDIR)/html/langdoc/jsdoc && mkdir -p $(BUILDDIR)/html/langdoc/javadoc

	  # kurento-client javadoc
	  cd  $(BUILDDIR)/langdoc && git clone https://github.com/Kurento/kurento-java.git && cd kurento-java && git checkout kurento-java-$(CLIENT_JAVA_VERSION)
	  rm -rf $(BUILDDIR)/langdoc/kurento-client
	  mv $(BUILDDIR)/langdoc/kurento-java/kurento-client $(BUILDDIR)/langdoc
	  cd $(BUILDDIR)/langdoc/kurento-client && mvn clean package -DskipTests
	  rsync -av $(BUILDDIR)/langdoc/kurento-client/target/generated-sources/kmd/* $(BUILDDIR)/langdoc/kurento-client/src/main/java/
	  javadoc -d $(BUILDDIR)/html/langdoc/javadoc -sourcepath $(BUILDDIR)/langdoc/*/src/main/java/ org.kurento.client org.kurento.client.factory

	  # kurento-client-js javadoc
	  rm -rf $(BUILDDIR)/langdoc/kurento-client-js
	  cd $(BUILDDIR)/langdoc && git clone https://github.com/Kurento/kurento-client-js.git
	  cd $(BUILDDIR)/langdoc/kurento-client-js git checkout $$kurento-client-js-$(CLIENT_JS_VERSION) && npm install && node_modules/.bin/grunt --force jsdoc
	  cp -r $(BUILDDIR)/langdoc/kurento-client-js/doc/jsdoc $(BUILDDIR)/html/langdoc/jsdoc/kurento-client-js

	  # kurento-utils-js javadoc
	  rm -rf $(BUILDDIR)/langdoc/kurento-utils-js
	  cd $(BUILDDIR)/langdoc && git clone https://github.com/Kurento/kurento-utils-js.git
	  cd $(BUILDDIR)/langdoc/kurento-utils-js git checkout $$kurento-utils-js-$(UTILS_JS_VERSION) &&  npm install && node_modules/.bin/grunt --force jsdoc
	  cp -r $(BUILDDIR)/langdoc/kurento-utils-js/doc/jsdoc $(BUILDDIR)/html/langdoc/jsdoc/kurento-utils-js

qthelp:
	$(SPHINXBUILD) -b qthelp $(ALLSPHINXOPTS) $(BUILDDIR)/qthelp
	@echo
	@echo "Build finished; now you can run "qcollectiongenerator" with the" \
	      ".qhcp project file in $(BUILDDIR)/qthelp, like this:"
	@echo "# qcollectiongenerator $(BUILDDIR)/qthelp/Kurento.qhcp"
	@echo "To view the help file:"
	@echo "# assistant -collectionFile $(BUILDDIR)/qthelp/Kurento.qhc"

devhelp:
	$(SPHINXBUILD) -b devhelp $(ALLSPHINXOPTS) $(BUILDDIR)/devhelp
	@echo
	@echo "Build finished."
	@echo "To view the help file:"
	@echo "# mkdir -p $$HOME/.local/share/devhelp/Kurento"
	@echo "# ln -s $(BUILDDIR)/devhelp $$HOME/.local/share/devhelp/Kurento"
	@echo "# devhelp"

epub:
	$(SPHINXBUILD) -b epub $(ALLSPHINXOPTS) $(BUILDDIR)/epub
	find build/epub -name "*.html" -exec sed -i -e "s@|DOC_VERSION|@$(DOC_VERSION)@" {} \;
	find build/epub -name "*.html" -exec sed -i -e "s@|CLIENT_JAVA_VERSION|@$(CLIENT_JAVA_VERSION)@" {} \;
	find build/epub -name "*.html" -exec sed -i -e "s@|CLIENT_JS_VERSION|@$(CLIENT_JS_VERSION)@" {} \;
	find build/epub -name "*.html" -exec sed -i -e "s@|UTILS_JS_VERSION|@$(UTILS_JS_VERSION)@" {} \;
	touch source/pdfindex.rst
	$(SPHINXBUILD) -b epub $(ALLSPHINXOPTS) $(BUILDDIR)/epub
	@echo
	@echo "Build finished. The epub file is in $(BUILDDIR)/epub."

latex:
	$(SPHINXBUILD) -b latex $(ALLSPHINXOPTS) $(BUILDDIR)/latex
	@echo
	@echo "Build finished; the LaTeX files are in $(BUILDDIR)/latex."
	@echo "Run \`make' in that directory to run these through (pdf)latex" \
	      "(use \`make latexpdf' here to do that automatically)."

latexpdf:
	$(SPHINXBUILD) -b latex $(ALLSPHINXOPTS) $(BUILDDIR)/latex
	@echo "Running LaTeX files through pdflatex..."
	find build/latex -name "*.tex" -exec sed -i -e "s@.textbar..DOC_VERSION.textbar..@$(DOC_VERSION)@" {} \;
	find build/latex -name "*.tex" -exec sed -i -e "s@.textbar..CLIENT_JAVA_VERSION.textbar..@$(CLIENT_JAVA_VERSION)@" {} \;
	find build/latex -name "*.tex" -exec sed -i -e "s@.textbar..CLIENT_JS_VERSION.textbar..@$(CLIENT_JS_VERSION)@" {} \;
	find build/latex -name "*.tex" -exec sed -i -e "s@.textbar..UTILS_JS_VERSION.textbar..@$(UTILS_JS_VERSION)@" {} \;
	$(MAKE) -C $(BUILDDIR)/latex all-pdf
	@echo "pdflatex finished; the PDF files are in $(BUILDDIR)/latex."

text:
	$(SPHINXBUILD) -b text $(ALLSPHINXOPTS) $(BUILDDIR)/text
	@echo
	@echo "Build finished. The text files are in $(BUILDDIR)/text."

man:
	$(SPHINXBUILD) -b man $(ALLSPHINXOPTS) $(BUILDDIR)/man
	@echo
	@echo "Build finished. The manual pages are in $(BUILDDIR)/man."

texinfo:
	$(SPHINXBUILD) -b texinfo $(ALLSPHINXOPTS) $(BUILDDIR)/texinfo
	@echo
	@echo "Build finished. The Texinfo files are in $(BUILDDIR)/texinfo."
	@echo "Run \`make' in that directory to run these through makeinfo" \
	      "(use \`make info' here to do that automatically)."

info:
	$(SPHINXBUILD) -b texinfo $(ALLSPHINXOPTS) $(BUILDDIR)/texinfo
	@echo "Running Texinfo files through makeinfo..."
	make -C $(BUILDDIR)/texinfo info
	@echo "makeinfo finished; the Info files are in $(BUILDDIR)/texinfo."

gettext:
	$(SPHINXBUILD) -b gettext $(I18NSPHINXOPTS) $(BUILDDIR)/locale
	@echo
	@echo "Build finished. The message catalogs are in $(BUILDDIR)/locale."

changes:
	$(SPHINXBUILD) -b changes $(ALLSPHINXOPTS) $(BUILDDIR)/changes
	@echo
	@echo "The overview file is in $(BUILDDIR)/changes."

linkcheck:
	$(SPHINXBUILD) -b linkcheck $(ALLSPHINXOPTS) $(BUILDDIR)/linkcheck
	@echo
	@echo "Link check complete; look for any errors in the above output " \
	      "or in $(BUILDDIR)/linkcheck/output.txt."

doctest:
	$(SPHINXBUILD) -b doctest $(ALLSPHINXOPTS) $(BUILDDIR)/doctest
	@echo "Testing of doctests in the sources finished, look at the " \
	      "results in $(BUILDDIR)/doctest/output.txt."

dist: langdoc html epub latexpdf
	mkdir -p $(BUILDDIR)/dist
	@echo
	@echo "Packaging documentation"
	@echo
	cp $(BUILDDIR)/epub/Kurento.epub $(BUILDDIR)/latex/Kurento.pdf $(BUILDDIR)/html &&\
	tar zcvf $(BUILDDIR)/dist/kurento-docs-$(DOC_VERSION).tgz -C $(BUILDDIR)/html .
