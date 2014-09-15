# Makefile for Sphinx documentation
#

# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = sphinx-build
PAPER         =
BUILDDIR      = build
JAVA_APIS     = kmf-media-api kmf-content-api kmf-repository-api
JS_APIS       = kws-media-api kws-content-api

# Internal variables.
PAPEROPT_a4     = -D latex_paper_size=a4
PAPEROPT_letter = -D latex_paper_size=letter
ALLSPHINXOPTS   = -d $(BUILDDIR)/doctrees $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) source
# the i18n builder cannot share the environment and doctrees with the others
I18NSPHINXOPTS  = $(PAPEROPT_$(PAPER)) $(SPHINXOPTS) source

.PHONY: help clean html dirhtml singlehtml pickle json htmlhelp javadoc qthelp devhelp epub latex latexpdf text man changes linkcheck doctest gettext dist

help:
	@echo "Please use \`make <target>' where <target> is one of"
	@echo "  javadoc    to make javadocs of the kurento APIs into"
	@echo "             source/kmf-*-api, source/langdocs/javadoc to be "
	@echo "             deployed from build/html/javadoc"
	@echo "  html       to make standalone HTML files"
	@echo "  dist       to make javadoc html epub latexpdf and then copy"
	@echo "             Kurento.{pdf,epub} in build/html and make a tgz"
	@echo "             as kurento-docs-$${VERSION}.tgz"
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
	export ver=$$(grep -E '^version =' source/conf.py | sed -e "s@.*'\(.*\)'@\\1@"); find build/html -name "*.html" -exec sed -i -e "s@|version|@$$ver@" {} \;
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
	- mkdir -p $(BUILDDIR)/langdoc && mkdir -p source/langdocs/jsdoc
	  rm -rf $(BUILDDIR)/kurento-media-framework
	  cd  $(BUILDDIR) && git clone https://github.com/Kurento/kurento-media-framework.git
	  for p in $(JAVA_APIS); do \
	      ( rm -rf $(BUILDDIR)/langdoc/$${p});\
	      ( mv $(BUILDDIR)/kurento-media-framework/$${p} $(BUILDDIR)/langdoc);\
	      done
	  for p in $(JS_APIS); do \
	      ( rm -rf $(BUILDDIR)/langdoc/$${p});\
	      ( cd  $(BUILDDIR)/langdoc && git clone https://github.com/Kurento/$${p}.git );\
	      done
	  for p in $(JAVA_APIS); do {\
	  export VERSION=$$(grep -E "release\s*=\s*['\"]" source/conf.py | sed -e "s@.*['\"]\(.*\)['\"]@\1@" );\
	  export CHECK=$$(echo $$VERSION | grep -- -dev >/dev/null && echo "develop" || echo "$${p}-$$VERSION");\
	      ( cd $(BUILDDIR)/langdoc/$${p} &&\
	        echo "Pulling repo $${p}, branch $${CHECK}..."; git checkout "$${CHECK}" || git checkout develop ) &&\
	      javasphinx-apidoc -c /tmp -u -T --no-member-headers -o source/$${p}\
	                                 "$$(cd $(BUILDDIR)/langdoc && pwd)/$${p}/src/main/java" \
	                                 $$(find $$(cd $(BUILDDIR)/langdoc && pwd)/$${p}\
	                                         -name internal -print -or -name tool -print  2>/dev/null);\
	      } done
		  javadoc -d source/langdocs/javadoc -sourcepath $$(echo $(BUILDDIR)/langdoc/k*/src/main/java | sed -e "s@ @:@g")\
		          -link http://tomcat.apache.org/tomcat-7.0-doc/servletapi \
		             com.kurento.kmf.media com.kurento.kmf.media.events com.kurento.kmf.media.params com.kurento.kmf.content com.kurento.kmf.repository
	  for p in $(JS_APIS); do {\
	  export VERSION=$$(grep -E "release\s*=\s*['\"]" source/conf.py | sed -e "s@.*['\"]\(.*\)['\"]@\1@" );\
	  export CHECK=$$(echo $$VERSION | grep -- -dev >/dev/null && echo "develop" || echo "$${p}-$$VERSION");\
	      ( cd $(BUILDDIR)/langdoc/$${p} &&\
	        echo "Pulling repo $${p}, branch $${CHECK}..."; git checkout "$${CHECK}" || git checkout develop  &&\
	         npm install && node_modules/.bin/grunt --force jsdoc ) && cp -r $(BUILDDIR)/langdoc/$${p}/doc/jsdoc source/langdocs/jsdoc/$${p} ;\
	      } done

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
	export ver=$$(grep -E '^version =' source/conf.py | sed -e "s@.*'\(.*\)'@\1@");\
	find build/epub -name "*.html" -exec sed -i -e "s@|version|@$$ver@" {} \;
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
	export ver=$$(grep -E '^version =' source/conf.py | sed -e "s@.*'\(.*\)'@\\1@") &&\
	find build/latex -name "*.tex" -exec sed -i -e "s@.textbar..version.textbar..@$$ver@" {} \;
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
	export VERSION=$$(grep -E "release\s*=\s*['\"]" source/conf.py | sed -e "s@.*['\"]\(.*\)['\"]@\1@" );\
	cp $(BUILDDIR)/epub/Kurento.epub $(BUILDDIR)/latex/Kurento.pdf $(BUILDDIR)/html &&\
	tar zcvf $(BUILDDIR)/dist/kurento-docs-$${VERSION}.tgz -C $(BUILDDIR)/html .
