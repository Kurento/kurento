%%%%%%%%%%%%%%
Code structure
%%%%%%%%%%%%%%

This project is organized into several modules:

- **kurento-repository-internal**: core Java library of repository. 
    Plain Java mostly, but with `Spring <https://spring.io/>`_ dependencies.
- **kurento-repository-server**: Wrapper for the library, 
    offering a simpler Java API and a Http REST API. Contains a 
    :term:`Spring Boot` application, very easy to setup and run.
- **kurento-repository-doc**: Module which generates this documentation using
    :term:`Sphinx`.