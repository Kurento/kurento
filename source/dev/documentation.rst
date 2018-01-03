=====================
Writing Documentation
=====================

[TODO]

The documentation for Kurento is built using Sphinx and hosted on Read the Docs. The docs are kept in the ``doc-kurento`` repository.

You can build the docs by installing Sphinx and running:

# in the docs directory
make html
Let us know if you have any questions or want to contribute to the documentation.

Documentation writer!

Use these reference links:

About reStructuredText:

- Quick Reference: http://docutils.sourceforge.net/docs/user/rst/quickref.html
- Summary by Sphinx: http://www.sphinx-doc.org/en/stable/rest.html

About Sphinx:

- Sphinx specific markup: http://www.sphinx-doc.org/en/stable/markup/index.html
- Types of references that can be used: http://www.sphinx-doc.org/en/stable/markup/inline.html#cross-referencing-syntax
- List of formats that can be used with the ``code-block`` directive: http://pygments.org/docs/lexers/

Use these title underline characters:

1. Level 1 (Document title). Use '=' above and below:

   .. code-block:: text

      =======
      Level 1
      =======

2. Level 2. Use '='.

   .. code-block:: text

      Level 2
      =======

3. Level 3. Use '-'.

   .. code-block:: text

      Level 3
      -------

4. Level 4. Use '~'.

   .. code-block:: text

      Level 4
      ~~~~~~~

5. Level 5. Use '"'.

   .. code-block:: text

      Level 5
      """""""
