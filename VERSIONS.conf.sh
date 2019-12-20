#!/usr/bin/env bash

# This file should be sourced by "configure.sh".

# PROJECT_VERSIONS is a Bash Associative Array. It contains all placeholder
# names and their values, that get replaced in the documentation sources.

# Normally, KMS is updated faster than other modules, so it is common that
# e.g. VERSION_KMS is 6.8.5 while most other modules still are at 6.8.0
# or even older.

declare -A PROJECT_VERSIONS=(
    # Version of the documentation itself; it appears in the main menu
    [VERSION_DOC]="6.13.0"

    # Version of Kurento Media Server
    [VERSION_KMS]="6.13.0"

    # Version of each Client API SDK
    [VERSION_CLIENT_JAVA]="6.13.0"
    [VERSION_CLIENT_JS]="6.13.0"

    # Version of the JavaScript utils module
    [VERSION_UTILS_JS]="6.13.0"

    # Version of each platform's tutorials
    [VERSION_TUTORIAL_JAVA]="6.13.0"
    [VERSION_TUTORIAL_JS]="6.13.0"
    [VERSION_TUTORIAL_NODE]="6.13.0"

    # Indicates if the current state of this code is Release or Nightly.
    # If "true", all dependent repos will be checked out to the corresponding
    # versions; otherwise, they will be kept at the default branch (master).
    [VERSION_RELEASE]="false"
)
