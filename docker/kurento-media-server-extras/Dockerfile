# ====================
# Kurento Media Server (modules extras)
# ====================
#
# This Docker image is used to run an instance of Kurento Media Server with modules-extras packages added
#
#
#
# Build Command
# =============
#
# Run:
#
#     docker build [Args...] --tag kurento/kurento-media-server-extras:latest .
#
#
#
# Build Arguments
# ---------------
#
# --build-arg KMS_VERSION=<KmsVersion>
#
#   <KmsVersion> is like "7.0.0".
#   Alternatively, "dev" is used to build a nightly version of Kurento.
#
#   Optional. Default: "dev".
#
# --build-arg APT_ARGS=<AptArguments>
#
#   <AptArguments> is a string with arguments that will be passed to all
#   executions of `apt-get`.
#
#   Example: To Use an Apt package proxy
#   Doc: http://manpages.ubuntu.com/manpages/man1/apt-transport-http.1.html#options
#
#       APT_ARGS='-o Acquire::http::Proxy=http://user:pass@host:port/'
#
#   Optional. Default: None.
#
#
#
# Run Command
# ===========
#
# Run:
#
#     docker run --name kurento --network host kurento/kurento-media-server-extras:latest
#
# Then, you can follow the logs with the `docker logs` command:
#
#     docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1


# Global arguments for FROM.
ARG KMS_VERSION="latest"


FROM kurento-media-server:${KMS_VERSION}

LABEL maintainer="Saúl Pablo Labajo Izquierdo <slabajo@naevatec.com>"

# Configure environment:
# Run apt-get/dpkg without interactive dialogue.
ARG DEBIAN_FRONTEND=noninteractive
# Set the default locale for all commands.
ENV LANG="C.UTF-8"

# Install extra modules.
# These might not be all available, so install separately and allow errors.
RUN \
apt-get $APT_ARGS update \
&& for PACKAGE in \
    kurento-module-videosampler ; \
do \
    apt-get $APT_ARGS install --no-install-recommends --yes "$PACKAGE" || true ; \
done \
&& rm -rf /var/lib/apt/lists/*

