==========================
Writing this documentation
==========================

.. contents:: Table of Contents

Although each of the Kurento repositories contains a *README* file, the main source of truth for up-to-date information about Kurento is *this* documentation you are reading right now. This is done in a 3 steps process:

1. Sources are written in a markup language called `reStructuredText <https://docutils.sourceforge.io/rst.html>`__ (or **reST**, for short). This format is less known that the popular `Markdown <https://www.markdownguide.org/getting-started/>`__, but it is much more powerful and adequate for long-form documentation writing.

   To learn the *reST* language have a look at the `Sphinx guide to reStructuredText <https://www.sphinx-doc.org/en/master/usage/restructuredtext/index.html>`__ and the official `Quick reStructuredText <https://docutils.sourceforge.io/docs/user/rst/quickref.html>`__ reference.

2. `Sphinx <https://www.sphinx-doc.org/>`__ is used to convert the *reST* files into the final output formal, such as HTML or PDF.

3. The resulting files are hosted in `Read the Docs <https://docs.readthedocs.io/>`__.

The core *reST* language is extended by Sphinx by means of *extensions*, and we use some of them:

* `sphinx.ext.extlinks <https://www.sphinx-doc.org/en/3.x/usage/extensions/extlinks.html>`__, to easily provide links to external sites (currently for the `English Wikipedia <https://en.wikipedia.org/>`__).
* `sphinx.ext.graphviz <https://www.sphinx-doc.org/en/3.x/usage/extensions/graphviz.html>`__, to embed `Graphviz <https://graphviz.org/>`__ DOT diagrams.
* `sphinx.ext.ifconfig <https://www.sphinx-doc.org/en/3.x/usage/extensions/ifconfig.html>`__, to conditionally build different blocks into the documentation.



Building locally
================

Check spelling
--------------

Please use a text editor that provides spell checking and live-preview visualization of *reST* files; this alone will help catching most grammatical and syntactic mistakes. `Visual Studio Code <https://code.visualstudio.com/>`__ is a great option, it provides extensions for both of these things:

.. code-block:: shell

   code --install-extension streetsidesoftware.code-spell-checker
   code --install-extension lextudio.restructuredtext



Install dependencies
--------------------

You'll need these tools:

* Python 3, including:

  - The *PIP* package installer, used to install Sphinx.
  - Optionally, but strongly recommended, the Python's virtual env tool.

* Graphviz, to convert ``.dot`` files into graphs.

.. code-block:: shell

   sudo apt-get update && sudo apt-get install --no-install-recommends \
       python3 python3-pip python3-venv \
       graphviz

Optionally, make a bit of cleanup in case old Sphinx versions were installed:

.. code-block:: shell

   # Ensure that old versions of Sphinx are not installed
   sudo apt-get purge --auto-remove \
       '^python-sphinx.*' \
       '^python3-sphinx.*' \
       '^sphinx.*'

   pip3 freeze | grep -i '^sphinx' | xargs sudo -H pip3 uninstall

And finally, install Sphinx:

.. code-block:: shell

   # Create and load the Python virtual environment
   python3 -m venv python_modules
   source python_modules/bin/activate

   # Install Sphinx and the Read the Docs theme
   python -m pip install --upgrade -r requirements.txt



Build docs
----------

Run ``make html`` inside the documentation directory, and open the newly built files with a web browser:

.. code-block:: shell

   # Load the Python virtual environment
   source python_modules/bin/activate

   # Build and open the documentation files
   make html
   firefox build/html/index.html



Kurento documentation Style Guide
=================================

Paragraph conventions
---------------------

* **Line breaks**: *Don't* break the lines. Documentation is prose text, and not source code, so the typical code line length limit rules don't make any sense and don't apply here.



Inline markup
-------------

* Names, acronyms, and in general any kind of referential name should be emphasized with single asterisks (as in ``*word*``).

* File names, full paths, URLs, package names, variable names, class and event names, code samples, commands, and in general any machine-oriented keywords, must be written inside double back-quotes (as in ````word````). This formatting *prevents line breaking*, which tends to be desirable for these kinds of technical words.

Sample phrases:

  .. code-block:: text

     This document talks about Kurento Media Server (*KMS*).
     All dependency targets are defined in the ``CMakeLists.txt`` file.
     You need to install ``libboost-dev`` for development.
     Enable debug by setting the ``GST_DEBUG`` environment variable.

     Use ``apt-get install`` to set up all required packages.
     Set ``CMAKE_BUILD_TYPE=Debug`` to build with debug symbols.
     The argument ``--gst-debug`` can be used to control the logging level.

Important differences between *reST* and *Markdown*:

* *reST* uses **two back-quotes** for inline code, not one. It is ````word````, not ```word```.

* *reST* renders *single asterisks* (``*word*``) and `single back-quotes` (```word```) as *italic text*. For this reason, it's better to always use asterisks for emphasizing, to avoid confusing people who come from Markdown.

* *reST* does *not* render underscores (as in ``_word_``), so don't use them to emphasize text.



Header conventions
------------------

* **Header separation**: Always separate each header from the preceding paragraph, by using **3** empty lines. The only exception to this rule is when two headers come together (e.g. a document title followed by a section title); in that case, they are separated by just **1** empty line.

* **Header shape**: *reST* allows to express section headers with any kind of characters that form an underline shape below the section title. We follow these conventions for Kurento documentation files:

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

Our Sphinx-based project is hosted in the `doc-kurento <https://github.com/Kurento/doc-kurento>`__ repository. Here, the main entry point for running Sphinx is the Makefile, based on the template that is provided for new projects by Sphinx itself. This Makefile is customized to attend our particular needs, and implements several targets:

* **init-workdir**. This target constitutes the first step to be run before most other targets. Our documentation source files contain substitution keywords in some parts, in the form ``| KEYWORD |``, which is expected to be substituted by some actual value during the generation process. Currently, the only keyword in use is ``VERSION``, which must be expanded to the actual version of the documentation being built.

  For example, here is the *VERSION_KMS* keyword when substituted with its final value: ``|VERSION_KMS|``.

  .. note::

     Sphinx already includes a substitutions feature by itself, for the keywords ``version`` and ``release``.  Sadly, this feature of Sphinx is very unreliable. For example, it won't work if the keyword is located inside a literal code block, or inside an URL. So, we must resort to performing the substitutions by ourselves during a pre-processing step, if we want reliable results.

  The way this works is that the *source* folder gets copied into the *build* directory, and then the substitutions take place over this copy.

* **langdoc**. This target creates the automatically generated reference documentation for each :doc:`/features/kurento_client`. Currently, this means the Javadoc and Jsdoc documentations for Java and Js clients, respectively. The Kurento client repositories are checked out in the same version as specified by the documentation version file, or in the master branch if no such version tag exists. Then, the client stubs of the :doc:`/features/kurento_modules` are automatically generated, and from the resulting source files, the appropriate documentation is automatically generated too.

  The *langdoc* target is usually run before the *html* target, in order to end up with a complete set of HTML documents that include all the reST documentation with the Javadoc/Jsdoc sections.

* **dist**. This target is a convenience shortcut to generate the documentation in the most commonly requested formats: HTML, PDF and EPUB. All required sub-targets will be run and the resulting files will be left as a compressed package in the ``dist/`` subdir.

* **ci-readthedocs**. This is a special target that is meant to be called exclusively by our Continuous Integration system. The purpose of this job is to manipulate all the documentation into a state that is a valid input for the Read the Docs CI system. Check the next section for more details.



Read the Docs builds
====================

It would be great if Read the Docs worked by simply calling the command *make html*, as then we would be able to craft a Makefile that would build the complete documentation in one single step (by making the Sphinx's *html* target dependent on our *init-workdir* and *langdoc*). But alas, they don't work like this; instead, they run Sphinx directly from their Python environment, rendering our Makefile as useless in their CI.

In order to overcome this limitation, we opted for the simple solution of handling RTD a specifically-crafted Git repository, with the contents that they expect to find. This works as follows:

1. Read the Docs has been configured to watch for changes in the `doc-kurento-readthedocs`_ repo, instead of *doc-kurento*.
2. The *init-workdir* and *langdoc* targets run locally from our *doc-kurento* repo.
3. The resulting files from those targets are copied as-is to the *doc-kurento-readthedocs* repository.
4. Everything is then committed and pushed to this latter repo, thus triggering a new RTD build.

.. _doc-kurento-readthedocs: https://github.com/Kurento/doc-kurento-readthedocs



robots.txt
----------

Read the Docs allows setting up a custom **robots.txt**, which we can use to prevent search engines from scrapping old and deprecated versions of the documentation, giving instead full priority to the ``/latest/`` and ``/stable/`` subdirectories in search engines:

* `How can I avoid search results having a deprecated version of my docs? <https://docs.readthedocs.io/en/stable/faq.html#how-can-i-avoid-search-results-having-a-deprecated-version-of-my-docs>`__.
* `Custom robots.txt Pages <https://docs.readthedocs.io/en/stable/hosting.html#custom-robots-txt-pages>`__.

This is exactly the behavior we want, because without it, searches like "kurento webrtc" would show results from old 6.9 or 6.10 pages, while we'd rather have the latest or stable versions appearing.
