ci-scripts
==========

Scripts used for all CI actions.

* All scripts that implement CI jobs (i.e. they are meant to be called directly by a CI workflow) start with `ci_job_`.
* Miscellaneous CI utils also start with `ci_`.
* All concrete operation scripts start with a recognizable keyword; for example, the scripts related to deployment of artifacts all start with `deploy_`.

Dependency graph:

![](README.d/scripts-dependencies.svg)
