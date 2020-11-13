==========================
Writing this documentation
==========================

Although each of the Kurento repositories contains a *README* file, the main source of truth for up-to-date information about Kurento is *this* documentation you are reading right now, hosted at Read the Docs.

The final deliverable form of the documentation is obtained by following a 3-step process:

1. The source files are written in a markup language called *reStructuredText* (reST). This format is less known that the popular Markdown, but it is much more powerful and adequate for long-form documentation writing.

2. The source files, written in *reST* format, are processed and converted to other deliverable formats by `Sphinx`_, a documentation processing tool which adds some layers of useful syntax to the *reST* baseline, and also takes care of generating all the documents in their final form.

.. _Sphinx: http://www.sphinx-doc.org/en/stable/index.html

3. Finally, the generated HTML files are hosted in Read The Docs, the service of choice for lots of open-source projects. Actually Read The Docs is not only the hosting, but they also perform the Sphinx generation step itself: they provide a Continuous Integration system that watches a Git repository and triggers a new documentation build each time it detects changes.

Kurento documentation files are written using both the basic features of *reST*, and the extra features that are provided by Sphinx. The *reST* language itself can be learned by checking any reference documents such as the `reStructuredText quick reference`_ and the `reStructuredText Primer`_.

.. _reStructuredText quick reference: http://docutils.sourceforge.net/docs/user/rst/quickref.html
.. _reStructuredText Primer: http://www.sphinx-doc.org/en/stable/rest.html

Sphinx adds its own set of useful markup elements, to make *reST* even more useful for writing documentation. To learn more about this, the most relevant section in their documentation is `Sphinx Markup Constructs`_.

.. _Sphinx Markup Constructs: http://www.sphinx-doc.org/en/stable/markup/index.html

Besides the extra markup added by Sphinx, there is also the possibility to use *Sphinx Extensions*, which each one does in turn add its own markup to extend even more the capabilities of the language. For example, as of this writing we are using `sphinx.ext.graphviz`_ and `sphinx-ext-wikipedia`_ extensions, to easily insert links to Wikipedia articles and embedded diagrams in the documents.

.. _sphinx.ext.graphviz: http://www.sphinx-doc.org/en/stable/ext/graphviz.html
.. _sphinx-ext-wikipedia: https://github.com/quiver/sphinx-ext-wikipedia



Building locally
================

If you are writing documentation for Kurento, there is no need to commit every change and push to see the result only to discover that an image doesn't show up as you expected, or some small mistake breaks a table layout.

- First of all, it's a very good idea to use a text editor that provides spell checking and live-preview visualization of *reST* files; this alone will help catching most grammatical and syntactic mistakes. `Visual Studio Code <https://code.visualstudio.com/>`__ is a great option, it provides extensions for both of these things.

  To install *VS Code* and the two mentioned functions, use `their installer <https://code.visualstudio.com/Download>`__, then run these commands:

  .. code-block:: console

     code --install-extension streetsidesoftware.code-spell-checker
     code --install-extension lextudio.restructuredtext

- Secondly, you can build the documentation locally. Just install the required dependencies:

  .. code-block:: console

     # Ensure that old versions of Sphinx are not installed
     sudo apt-get purge --auto-remove \
         python-sphinx \
         sphinx-common \
         python-sphinx-rtd-theme \
         sphinx-rtd-theme-common

  .. code-block:: console

     # Ensure that Sphinx is not installed with Python 3
     sudo pip3 uninstall sphinx
     sudo pip3 uninstall sphinx_rtd_theme
     sudo pip3 uninstall sphinxcontrib_websupport

  .. code-block:: console

     # Install Sphinx with Python 2
     sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
         python-pip \
         python-setuptools

     sudo pip2 install --upgrade sphinx sphinx_rtd_theme

  And then just run ``make html`` to build and open your new shiny files:

  .. code-block:: console

     cd doc-kurento/
     make html
     firefox build/html/index.html



Kurento documentation Style Guide
=================================

Paragraph conventions
---------------------

- **Line breaks**: *Don't* break the lines. The documentation is a prose text, and not source code, so the typical restrictions of line length don't apply here. Use automatic line breaks in your editor, if you want. The overall flow of the text should be dictated by the width of the screen where the text is being presented, and not by some arbitrary line length limit.



Inline markup
-------------

- File names, package names, variable names, class and event names, (mostly all kinds of names), acronyms, commit hashes, and in general any kind of identifier which could be broken into different lines are emphasized with single asterisks (as in ``*word*``). Sample phrases:

  .. code-block:: text

     This document talks about Kurento Media Server (*KMS*).
     All dependency targets are defined in the *CMakeLists.txt* file.
     You need to install *libboost-dev* for development.
     Enable debug by setting the *GST_DEBUG* environment variable.

- Paths, URLs, code samples, commands, and in general any machine-oriented keywords are emphasized with double back quotes (as in ````word````). This formatting stands out, and most importantly *it cannot be broken into different lines*. Sample phrases:

  .. code-block:: text

     Use ``apt-get install`` to set up all required packages.
     Set ``CMAKE_BUILD_TYPE=Debug`` to build with debug symbols.
     The argument ``--gst-debug`` can be used to control the logging level.

- There is no difference between using *single asterisks* (``*word*``), and `single back quotes` (```word```); they get rendered as *italic text*. So, always use asterisks when wanting to emphasize some text.

- As opposed to Markdown, underscores (as in ``_word_``) *don't get rendered*, so don't use them to emphasize text.



Header conventions
------------------

- **Header separation**: Always separate each header from the preceding paragraph, by using **3** empty lines. The only exception to this rule is when two headers come together (e.g. a document title followed by a section title); in that case, they are separated by just **1** empty line.

- **Header shape**: *reST* allows to express section headers with any kind of characters that form an underline shape below the section title. We follow these conventions for Kurento documentation files:

  1. Level 1 (Document title). Use ``=`` above and below:

  .. code-block:: text

        =======
        Level 1
        =======

  2. Level 2. Use ``=`` below:

  .. code-block:: text

        Level 2
        =======

  3. Level 3. Use ``-``:

  .. code-block:: text

        Level 3
        -------

  4. Level 4. Use ``~``:

  .. code-block:: text

        Level 4
        ~~~~~~~

  5. Level 5. Use ``"``:

  .. code-block:: text

        Level 5
        """""""



Sphinx documentation generator
==============================

Our Sphinx-based project is hosted in the `doc-kurento`_ repository. Here, the main entry point for running Sphinx is the Makefile, based on the template that is provided for new projects by Sphinx itself. This Makefile is customized to attend our particular needs, and implements several targets:

.. _doc-kurento: https://github.com/Kurento/doc-kurento

- **init-workdir**. This target constitutes the first step to be run before most other targets. Our documentation source files contain substitution keywords in some parts, in the form ``| KEYWORD |``, which is expected to be substituted by some actual value during the generation process. Currently, the only keyword in use is ``VERSION``, which must be expanded to the actual version of the documentation being built.

  For example, here is the *VERSION_KMS* keyword when substituted with its final value: ``|VERSION_KMS|``.

  Yes, Sphinx does already include a substitutions feature by itself, and the keyword ``VERSION`` is precisely one of the supported substitutions. Sadly, this feature of Sphinx is very unreliable. For example, it won't work if the keyword is located inside a literal code block, or inside an URL. So, we must resort to performing the substitutions by ourselves if we want reliable results.

  The *source* folder is copied into the *build* directory, and then the substitutions take place over this copy.

- **langdoc**. This target creates the automatically generated reference documentation for each :doc:`/features/kurento_client`. Currently, this means the Javadoc and Jsdoc documentations for Java and Js clients, respectively. The Kurento client repositories are checked out in the same version as specified by the documentation version file, or in the master branch if no such version tag exists. Then, the client stubs of the :doc:`/features/kurento_api` are automatically generated, and from the resulting source files, the appropriate documentation is automatically generated too.

  The *langdoc* target is usually run before the *html* target, in order to end up with a complete set of HTML documents that include all the reST documentation with the Javadoc/Jsdoc sections.

- **dist**. This target is a convenience shortcut to generate the documentation in the most commonly requested formats: HTML, PDF and EPUB. All required sub-targets will be run and the resulting files will be left as a compressed package in the *dist/* subfolder.

- **ci-readthedocs**. This is a special target that is meant to be called exclusively by our Continuous Integration system. The purpose of this job is to manipulate all the documentation into a state that is a valid input for the Read The Docs CI system. Check the next section for more details.



Read the Docs builds
====================

It would be great if Read the Docs worked by simply calling the command *make html*, as then we would be able to craft a Makefile that would build the complete documentation in one single step (by making the Sphinx's *html* target dependent on our *init-workdir* and *langdoc*). But alas, they don't work like this; instead, they run Sphinx directly from their Python environment, rendering our Makefile as useless in their CI.

In order to overcome this limitation, we opted for the simple solution of handling RTD a specifically-crafted Git repository, with the contents that they expect to find. This works as follows:

1. Read the Docs has been configured to watch for changes in the `doc-kurento-readthedocs`_ repo, instead of *doc-kurento*.
2. The *init-workdir* and *langdoc* targets run locally from our *doc-kurento* repo.
3. The resulting files from those targets are copied as-is to the *doc-kurento-readthedocs* repository.
4. Everything is then committed and pushed to this latter repo, thus triggering a new RTD build.

.. _doc-kurento-readthedocs: https://github.com/Kurento/doc-kurento-readthedocs
