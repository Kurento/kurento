#!/usr/bin/env bash

# This file should be sourced by the project's `make` script.

# PROJECT_VERSIONS is a Bash Associative Array. Contains all placeholder names
# and their values.

# Normally, KMS is updated faster than other modules, so it is common that
# e.g. KMS is on version 6.8.5 while most other modules still are at 6.8.0
# or even older.
# These values get replaced in the documentation sources.

declare -A PROJECT_VERSIONS=(
    # Version of the documentation itself; it appears in the main menu
    [VERSION_DOC]="6.11.0"

    # Version of Kurento Media Server
    [VERSION_KMS]="6.11.0"

    # Version of each Client API SDK
    [VERSION_CLIENT_JAVA]="6.11.0"
    [VERSION_CLIENT_JS]="6.11.0"

    # Version of the JavaScript utils module
    [VERSION_UTILS_JS]="6.11.0"

    # Version of each platform's tutorials
    [VERSION_TUTORIAL_JAVA]="6.11.0"
    [VERSION_TUTORIAL_JS]="6.11.0"
    [VERSION_TUTORIAL_NODE]="6.11.0"
)
