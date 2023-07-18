#!/usr/bin/env bash

#/ Prepare a Docker container with a full development environment for Kurento.
#/
#/ NOTE: This might evolve over time and become a proper devcontainer:
#/
#/ * https://containers.dev/
#/ * https://code.visualstudio.com/docs/devcontainers/containers
#/
#/
#/ Arguments
#/ =========
#/
#/ <DistribRelease>
#/
#/   Version number of the target Ubuntu system. E.g. "20.04" for Ubuntu Focal.
#/
#/   Required. As of this writing, Kurento 6.x works with Ubuntu 16.04 and 18.04,
#/   while Kurento 7.x works with Ubuntu 20.04.



# Shell setup
# ===========

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
#set -o xtrace

# Exit trap function.
# Runs always at the end, either on success or error (errexit).
function on_exit {
    { _RC=$?; set +o xtrace; } 2>/dev/null
    if ((_RC)); then echo "ERROR ($_RC)"; fi
}
trap on_exit EXIT

# Help message.
# Extracts and prints text from special comments in the script header.
function usage { grep '^#/' "${BASH_SOURCE[-1]}" | cut -c 4-; exit 0; }
if [[ "${1:-}" =~ ^(-h|--help)$ ]]; then usage; fi



# Parse call arguments
# ====================

CFG_DISTRIB_RELEASE=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        *)
            CFG_DISTRIB_RELEASE="$1"
            ;;
    esac
    shift
done



# Validate config
# ===============

if [[ -z "$CFG_DISTRIB_RELEASE" ]]; then
    echo "ERROR: Missing expected <DistribRelease>"
    echo "Run with '--help' to read usage details"
    exit 1
fi

echo "CFG_DISTRIB_RELEASE=$CFG_DISTRIB_RELEASE"



# Create Docker container
# =======================

# In-place Docker container commands BEGIN
docker run -i --pull always \
    --name "kurento-dev-$CFG_DISTRIB_RELEASE" \
    "ubuntu:$CFG_DISTRIB_RELEASE" \
    <<'DOCKERCOMMANDS'
# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
#set -o xtrace

# Get DISTRIB_* env vars.
source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release



echo "# Apt config"
# =================

APT_MIRROR="http://archive.ubuntu.com/ubuntu/"
#APT_MIRROR="http://mirror.tedra.es/ubuntu/"
#APT_MIRROR="http://ubuntu.cica.es/ubuntu/"

tee /etc/apt/sources.list >/dev/null <<EOF
deb $APT_MIRROR $DISTRIB_CODENAME main restricted universe multiverse
deb $APT_MIRROR $DISTRIB_CODENAME-updates main restricted universe multiverse
deb $APT_MIRROR $DISTRIB_CODENAME-backports main restricted universe multiverse
deb http://security.ubuntu.com/ubuntu/ $DISTRIB_CODENAME-security main restricted universe multiverse
#deb http://archive.canonical.com/ubuntu/ $DISTRIB_CODENAME partner

#deb-src $APT_MIRROR $DISTRIB_CODENAME main restricted universe multiverse
#deb-src $APT_MIRROR $DISTRIB_CODENAME-updates main restricted universe multiverse
#deb-src $APT_MIRROR $DISTRIB_CODENAME-backports main restricted universe multiverse
#deb-src http://security.ubuntu.com/ubuntu/ $DISTRIB_CODENAME-security main restricted universe multiverse
#deb-src http://archive.canonical.com/ubuntu/ $DISTRIB_CODENAME partner
EOF

# Disable automatic installation of "recommended" and "suggested" dependencies.
tee /etc/apt/apt.conf.d/99no-recommends >/dev/null <<EOF
APT::Install-Recommends "false";
APT::Install-Suggests "false";
EOF

# Disable IPv6, which has caused download issues in the past.
tee /etc/apt/apt.conf.d/99force-ipv4 >/dev/null <<EOF
Acquire::ForceIPv4 "true";
EOF

# Disable automatic cleaning of the apt cache, to keep downloaded packages.
sed -i 's|^|//|' /etc/apt/apt.conf.d/docker-clean

# Disable installation of manpages and other unneeded files.
# See https://wiki.ubuntu.com/ReducingDiskFootprint#Documentation
mkdir -p /etc/dpkg/dpkg.cfg.d/
tee /etc/dpkg/dpkg.cfg.d/01_nodoc >/dev/null <<EOF
path-exclude /usr/share/doc/*
# we need to keep copyright files for legal reasons
path-include /usr/share/doc/*/copyright
path-exclude /usr/share/man/*
path-exclude /usr/share/groff/*
path-exclude /usr/share/info/*
# lintian stuff is small, but really unnecessary
path-exclude /usr/share/lintian/*
path-exclude /usr/share/linda/*
EOF



echo "# Tools config"
# ===================

# bash-completion
sed -i '/bash_completion$/s/^#//g' "$HOME/.bashrc"

# tzdata: Avoid interactive questions during `apt-get install`.
TZ="Europe/Madrid"
ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime
echo "$TZ" >/etc/timezone

# ccache (GCC build cache)
tee /etc/ccache.conf >/dev/null <<EOF
max_size = 10.0G
run_second_cpp = true
EOF
tee -a "$HOME/.profile" >/dev/null <<'EOF'
# ccache (GCC build cache)
export PATH="/usr/lib/ccache:$PATH"
EOF

# make
tee -a "$HOME/.profile" >/dev/null <<'EOF'
# make parallel jobs (check with `nproc`)
export MAKEFLAGS="-j$(nproc)"
EOF

# valgrind
# Put custom builds of Valgrind in /opt/, and update the value here:
tee -a "$HOME/.profile" >/dev/null <<'EOF'
# Valgrind
DIR="/opt/valgrind-3.15.0"
export PATH="$DIR/bin:$PATH"
export LD_LIBRARY_PATH="$DIR/lib:$LD_LIBRARY_PATH"
export PKG_CONFIG_PATH="$DIR/lib/pkgconfig:$PKG_CONFIG_PATH"
EOF



echo "# Install basic tools"
# ==========================

PACKAGES=(
    apt-file
    aptitude
    bash-completion
    git
    iproute2 # ip
    iputils-ping
    less
    mlocate # updatedb, locate
    nano
    net-tools # netstat, ifconfig
    procps
    psmisc # killall
    tree
    wget
)

apt-get update ; apt-get install --no-install-recommends --yes "${PACKAGES[@]}"

apt-file update



echo "# Install development tools"
# ================================

PACKAGES=(
    # Compilation and debugging.
    build-essential
    ccache
    gdb

    # Java + Maven.
    default-jdk-headless
    maven
)

apt-get update ; apt-get install --no-install-recommends --yes "${PACKAGES[@]}"



echo "# (Optional) Install tools for kurento-buildpackage"
# ========================================================

PACKAGES=(
    # System tools.
    coreutils
    lsb-release

    # Dpkg tools.
    devscripts
    dpkg-dev
    equivs
    lintian

    # Git + SSL/HTTPS.
    git
    openssh-client

    # Python.
    python3
    python3-pip
    python3-setuptools
    python3-wheel
)

apt-get update ; apt-get install --no-install-recommends --yes "${PACKAGES[@]}"

git config --system user.name "Kurento"
git config --system user.email "kurento@openvidu.io"
git config --global --add safe.directory '*'

if [[ "$DISTRIB_RELEASE" == "16.04" ]]; then
    # Use official installer because system version is broken.
    apt-get purge --auto-remove --yes python3-pip
    wget -O /tmp/get-pip.py https://bootstrap.pypa.io/pip/3.5/get-pip.py
    python3 /tmp/get-pip.py

    # Install old version because newer ones are not compatible.
    # gbp>=0.9.14 requires git>=2.10, because is uses `git --no-show-signature`.
    # Workaround: install gbp==0.9.13... but gbp between 0.9.11 and 0.9.13 had a
    # broken install script! 0.9.10 is the most recent that can be installed :-(
    python3 -m pip install --upgrade nosexcover
    python3 -m pip install --upgrade --use-deprecated=legacy-resolver gbp==0.9.10
else
    python3 -m pip install --upgrade gbp
fi



echo "# (Optional) Install Kurento Media Server"
# ==============================================

apt-get update ; apt-get install --no-install-recommends --yes \
    gnupg

# Add Kurento repository key for apt-get.
apt-key adv \
    --keyserver hkp://keyserver.ubuntu.com:80 \
    --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83

# Add Kurento repository line for apt-get.
if [[ "$DISTRIB_RELEASE" == "20.04" ]]; then
    # Repo for Kurento 7 monorepo.
    echo "deb [arch=amd64] http://ubuntu.openvidu.io/dev $DISTRIB_CODENAME main" \
        >/etc/apt/sources.list.d/kurento.list

    # Repo for Kurento 7 multirepo.
    #echo "deb [arch=amd64] http://ubuntu.openvidu.io/dev-7.0.0 $DISTRIB_CODENAME kms6" \
    #    >/etc/apt/sources.list.d/kurento.list
else
    echo "deb [arch=amd64] http://ubuntu.openvidu.io/dev $DISTRIB_CODENAME kms6" \
        >/etc/apt/sources.list.d/kurento.list
    echo "#deb [arch=amd64] http://ubuntu.openvidu.io/6.18.0 $DISTRIB_CODENAME kms6" \
        >>/etc/apt/sources.list.d/kurento.list
fi

# Install Kurento Media Server.
apt-get update ; apt-get install --no-install-recommends --yes \
    kurento-media-server

# Install Kurento Media Server development packages.
apt-get update ; apt-get install --no-install-recommends --yes \
    kurento-media-server-dev

# Run Kurento, method 1: with service scripts.
#service kurento-media-server restart

# Run Kurento, method 2: by hand.
#source /etc/default/kurento-media-server
#unset GST_DEBUG_NO_COLOR
#unset KURENTO_LOGS_PATH
#kurento-media-server # (Optional) Use `exec` to replace root PID



echo "# (Optional) Install debug symbols"
# =======================================

apt-get update ; apt-get install --no-install-recommends --yes \
    gnupg

# Add Ubuntu debug repository key for apt-get.
apt-get update ; apt-get install --yes ubuntu-dbgsym-keyring \
|| apt-key adv \
    --keyserver hkp://keyserver.ubuntu.com:80 \
    --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622

# Add Ubuntu debug repository line for apt-get.
tee /etc/apt/sources.list.d/ddebs.list >/dev/null <<EOF
deb http://ddebs.ubuntu.com $DISTRIB_CODENAME main restricted universe multiverse
deb http://ddebs.ubuntu.com ${DISTRIB_CODENAME}-updates main restricted universe multiverse
EOF

# Install debug packages.
# The debug packages repository fails very often due to bad server state.
# Try to update, and only if it works, install debug symbols. Otherwise, disable
# the debug repository.
apt-get update && apt-get install --no-install-recommends --yes kurento-dbg \
|| {
    echo "WARNING: Ubuntu debug repository is down! Leaving it disabled"
    sed -i 's/^[^#]/#&/' /etc/apt/sources.list.d/ddebs.list
}



echo "# End cleanup"
# ==================

rm -rf /var/cache/apt/*
rm -rf /var/lib/apt/lists/*

echo "Done! Everything got installed successfully"

DOCKERCOMMANDS
# In-place Docker container commands END



echo "# Create Docker image"
# ==========================

docker commit "kurento-dev-$CFG_DISTRIB_RELEASE" "kurento-dev:$CFG_DISTRIB_RELEASE"
docker container rm "kurento-dev-$CFG_DISTRIB_RELEASE"



echo "# Print usage examples"
# ===========================

cat <<EOF
To start a new dev container:

    # CHANGE THIS. Use your Kurento development root path.
    cd ~/work/

    docker run -it --name kurento-dev-$CFG_DISTRIB_RELEASE \\
        --network host \\
        -v "\$PWD":/hostdir \\
        -v /opt:/opt \\
        --cap-add SYS_PTRACE --security-opt seccomp=unconfined \\
        --ulimit core=-1 \\
        kurento-dev:$CFG_DISTRIB_RELEASE \\
        bash --login

Notes:
* Mounts /opt/ to access extra software manually built (eg. Valgrind)
* --cap-add=SYS_PTRACE --security-opt seccomp=unconfined:
  Allows running GDB or AddressSanitizer inside the Docker container.
* --ulimit core=-1:
  Allows generation of core dumps by the kernel. -1 means "unlimited".

To run 'kurento-buildpackage' inside the container:

    /hostdir/kurento/ci-scripts/kurento-buildpackage.sh \\
        --install-files /hostdir/packages \\
        --dstdir /hostdir/packages

To build and run Kurento Media Server from source code:

    cd /hostdir/kurento/server/
    bin/build-run.sh
EOF
