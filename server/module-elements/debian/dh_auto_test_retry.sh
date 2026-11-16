#!/usr/bin/env bash

# Run the package tests (dh_auto_test, which calls CTest), retrying ONLY the
# tests that are known to fail intermittently.
#
# Rationale: test_webrtcendpoint crashes at random (SIGSEGV / SIGABRT from
# threads that use OpenSSL / DTLS, heap corruption) in about 1 of every 3 runs,
# also on unmodified code. Until that bug is fixed, a single random crash
# should not break the whole package build.
#
# Behavior:
# - The test suite runs normally the first time.
# - If tests fail, and ALL the failed tests match FLAKY_TEST_REGEX, the failed
#   tests (and only those) are run again, up to FLAKY_TEST_MAX_ATTEMPTS runs in
#   total. Each retry is announced in the log with a "FLAKY-TEST-RETRY" line,
#   and a test that finally passes is reported with "FLAKY-TEST-PASSED-ON-RETRY".
# - If any other test fails, or a flaky test fails in all attempts, this script
#   exits with an error and the build fails.
# - DEB_BUILD_OPTIONS=nocheck is honored (dh_auto_test does nothing).
#
# This file must be removed once the underlying bug is fixed.
#
# Environment variables:
#   FLAKY_TEST_REGEX         Extended regex matched against the *whole* CTest
#                            test name. Default: ^test_webrtcendpoint$
#   FLAKY_TEST_MAX_ATTEMPTS  Max runs per flaky test, 1 = no retries. Default: 3

set -o nounset
set -o pipefail

FLAKY_TEST_REGEX="${FLAKY_TEST_REGEX:-^test_webrtcendpoint\$}"
FLAKY_TEST_MAX_ATTEMPTS="${FLAKY_TEST_MAX_ATTEMPTS:-3}"
DH_AUTO_TEST="${DH_AUTO_TEST:-dh_auto_test}"

LOG="$(mktemp)"
trap 'rm -f "$LOG"' EXIT

# Print the names of the failed tests from a CTest log. CTest ends its summary
# with a block like:
#   The following tests FAILED:
#   	 12 - test_webrtcendpoint (Failed)
failed_tests() {
    sed -n '/^The following tests FAILED:/,$p' "$LOG" \
        | sed -nE 's/^[[:space:]]*[0-9]+ - ([^ ]+) \(.*\)$/\1/p'
}

attempt=1
$DH_AUTO_TEST 2>&1 | tee "$LOG"
rc="${PIPESTATUS[0]}"
first_failed=""

while [[ "$rc" -ne 0 ]]; do
    mapfile -t failed < <(failed_tests)
    if [[ "${#failed[@]}" -eq 0 ]]; then
        echo "FLAKY-TEST-RETRY: tests failed (exit code $rc) but no failed test names were found in the CTest output: NOT retrying."
        exit "$rc"
    fi
    [[ -z "$first_failed" ]] && first_failed="${failed[*]}"

    for t in "${failed[@]}"; do
        if ! [[ "$t" =~ $FLAKY_TEST_REGEX ]]; then
            echo "FLAKY-TEST-RETRY: test '$t' failed and is not in the list of known flaky tests (regex '$FLAKY_TEST_REGEX'): NOT retrying, failing the build."
            exit "$rc"
        fi
    done

    if [[ "$attempt" -ge "$FLAKY_TEST_MAX_ATTEMPTS" ]]; then
        echo "FLAKY-TEST-RETRY: known flaky test(s) '${failed[*]}' still failing after $attempt attempt(s): failing the build."
        exit "$rc"
    fi

    attempt=$((attempt + 1))
    echo "FLAKY-TEST-RETRY: known flaky test(s) '${failed[*]}' failed; re-running only the failed tests (attempt $attempt of $FLAKY_TEST_MAX_ATTEMPTS)."
    $DH_AUTO_TEST -- --rerun-failed 2>&1 | tee "$LOG"
    rc="${PIPESTATUS[0]}"
done

if [[ "$attempt" -gt 1 ]]; then
    echo "FLAKY-TEST-PASSED-ON-RETRY: '$first_failed' failed on the first run and passed after $attempt attempts."
fi
exit 0
