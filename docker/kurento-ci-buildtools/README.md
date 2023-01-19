# kurento-ci-buildtools

Image source: [Dockerfile](https://github.com/Kurento/kurento/blob/main/docker/kurento-ci-buildtools/Dockerfile).

This Docker image is used to run all CI jobs related to Kurento projects.

It extends [kurento/kurento-buildpackage](https://hub.docker.com/r/kurento/kurento-buildpackage), to add all sorts of tools needed for different programming language jobs, such as Maven for Java projects, and Node.js for JavaScript projects.
