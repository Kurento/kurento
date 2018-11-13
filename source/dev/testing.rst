:orphan:

..
   Hidden section. When some contents are added:
   - Remove the :orphan: tag
   - Remove this comment
   - Un-comment the section's name in the index file

=======
Testing
=======

Software testing is a broad term within software engineering encompassing a wide spectrum of different concepts. Depending on the size of the System Under Test (SUT) and the scenario in which it is exercised, testing can be carried out at different levels. There is no universal classification for all the different testing levels. Nevertheless, the following levels are broadly accepted:

- Unit: individual program units are tested. Unit tests typically focus on the functionality of individual objects or methods.
- Integration: units are combined to create composite components. Integration tests focus on the interaction of different units.
- System: all of the components are integrated and the system is tested as a whole.

There is a special type of system tests called **end-to-end** (E2E). In E2E tests, the final user is typically impersonated, i.e., simulated using automation techniques. The main benefit of E2E tests is the simulation of real user scenarios in an automated fashion. Nevertheless, this kind tests have several deterrents. For instance, end-to-end tests are challenging in terms of automation since a proper infrastructure to carry out user impersonation (e.g. using web browsers). In addition, E2E tests does not isolate failures (such as unit tests), and therefore, trace a failed E2E test is usually costly. In order to overcome these problems for testing Kurento, the **Kurento Testing Framework** (KTF) has been created.

Kurento Testing Framework explained
===================================

This section explain how KTF works.

E2E Tests
=========

This section introduces the different types of E2E implmemented with KTF for Kurento.

Running tests
=============

This section explains the KTF API.

In local environemnt
--------------------

This section explains how to use the KTF API for running Kurento tests in a local environment.

In Jenkins
----------

This section explains how to use the KTF API for running Kurento tests in a Jenkins Continuous Integration (CI) server.
