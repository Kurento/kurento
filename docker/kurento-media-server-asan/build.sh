#!/usr/bin/env bash

#/ Docker script - Build Kurento Media Server with AddressSanitizer.
#/
#/
#/ Environment variables
#/ ---------------------
#/
#/ KMS_VERSION=<KmsVersion>
#/
#/   <KmsVersion> is like "7.0.0".
#/   Alternatively, "dev" is used to build a nightly version of KMS.
#/
#/   Required. Default: "0.0.0" (invalid version).

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# Download
git clone https://github.com/Kurento/kurento.git
pushd kurento/server/  # Enter server/
if [[ "$KMS_VERSION" != "dev" ]]; then
    git checkout "$KMS_VERSION" || true
fi
git submodule update --init --recursive



# Fix GCC 9 compilation error
sed -i 's/return gobject_/return \&gobject_/' \
    /usr/include/glibmm-2.4/glibmm/threads.h



# Build

# Use flag recommended for aggressive diagnostics:
# https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
CFLAGS="${CFLAGS:-} -fsanitize-address-use-after-scope" \
CXXFLAGS="${CXXFLAGS:-} -fsanitize-address-use-after-scope" \
bin/build-run.sh --verbose --build-only --address-sanitizer

cd build-*/
KMS_BIN_DIR="$PWD"
popd  # Exit server/



# Dist
mkdir /kurento-asan
find "$KMS_BIN_DIR" -type f -name '*.so*' -exec cp -av {} /kurento-asan/ \;
cp -av "$KMS_BIN_DIR/media-server/server/kurento-media-server" \
    /kurento-asan/
cp -avL "/usr/lib/gcc/x86_64-linux-gnu/$(gcc -dumpversion | grep -Po '^\d+')/libasan.so" \
    /kurento-asan/
