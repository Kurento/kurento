#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

# Delete all workspace contents of all GitHub Actions runners.



# Shell setup
# ===========

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
#set -o xtrace



# Settings
# ========

# Runner label. Used as label and also as prefix for runner names.
# Must be a single word (no spaces allowed).
RUNNER_LABEL="$(echo "${RUNNER_LABEL:-kurento}" | tr -d '[:space:]')"

# Absolute path to the parent dir where runners will be configured.
RUNNER_HOME="${RUNNER_HOME:-$HOME/$RUNNER_LABEL-github-runners/}"



# Cleanup
# =======

find "$RUNNER_HOME" -path '*/workspace/*/kurento/kurento' -type d -print -exec rm -rf '{}' \+

echo "Done!"
