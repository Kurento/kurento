ci-scripts
==========

Scripts used for all CI actions.

* All scripts that implement CI jobs (i.e. they are meant to be called directly by a CI workflow) start with `ci_job_`.
* Miscellaneous CI utils also start with `ci_`.
* All concrete operation scripts start with a recognizable keyword; for example, the scripts related to deployment of artifacts all start with `deploy_`.

Docker image caching: the Docker image deployment jobs (`ci_job_deploy_docker*.sh`) pass the build argument `KMS_APT_STATE` to `docker buildx build`. Its value is a digest of the `Release` file of the Apt repository the image installs from (see `get_apt_repo_state.sh`). It makes the Docker layers that install Kurento packages be rebuilt whenever new packages are published, while the layers before them (base tools, repository key) stay cached. Without it, BuildKit would reuse the cached layer and publish an image with an outdated package.

Dependency graph:

![](README.d/scripts-dependencies.svg)
