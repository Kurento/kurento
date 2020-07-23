#!/usr/bin/env bash

# Preparation script to build ALVAR from sources.
#
# ALVAR source code has some issues with modern versions of the compiler, so
# we maintain a patch that fixes all compilation errors. This patch should be
# applied once before the build step.

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Apply our patch on the ALVAR sources
git apply --verbose \
    --directory=alvar-2.0.0-src-opencv3 \
    ./alvar-compat-opencv-2-3.patch
