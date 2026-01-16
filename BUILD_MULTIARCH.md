# Multi-Architecture Build Guide

This repository supports building Kurento Media Server for both **amd64** and **arm64** architectures.

## Prerequisites

- Docker with buildx support
- Sufficient disk space (~10GB per architecture)
- QEMU configured for multi-platform builds (usually automatic with Docker Desktop)

## Quick Start

### Build for Both Architectures

```bash
# Build all components for amd64 and arm64
./build_all_docker.sh

# Output will be in:
# - server/packages/amd64/
# - server/packages/arm64/
```

### Build for Single Architecture

```bash
# Build for amd64 only
./build_all_docker.sh all amd64

# Build for arm64 only
./build_all_docker.sh all arm64
```

### Build Specific Component

```bash
# Build media-server for both architectures
./build_all_docker.sh media-server

# Build media-server for amd64 only
./build_all_docker.sh media-server amd64
```

## Creating Docker Images

After building packages, create runtime Docker images:

```bash
# Create images for both architectures
./create_docker_image_from_packages.sh multi

# Creates:
#   kurento-media-server:local-amd64
#   kurento-media-server:local-arm64
```

Or for a single architecture:

```bash
./create_docker_image_from_packages.sh amd64
./create_docker_image_from_packages.sh arm64
```

## Running Docker Images

```bash
# Run amd64 image
docker run --rm --network host kurento-media-server:local-amd64

# Run arm64 image
docker run --rm --network host kurento-media-server:local-arm64
```

## How It Works

1. **Docker Buildx**: Uses Docker's buildx feature to build for multiple platforms
2. **Native Compilation**: Each architecture builds natively in its own container (using QEMU emulation if needed)
3. **Separate Outputs**: Packages are organized by architecture in `server/packages/<arch>/`
4. **Build Container**: Creates architecture-specific build containers (`kurento-buildpackage:amd64`, `kurento-buildpackage:arm64`)

## Build Process Details

### Step 1: Build Container Creation
```bash
# Automatically creates build containers for each architecture
docker buildx build --platform linux/amd64 -t kurento-buildpackage:amd64 docker/kurento-buildpackage/
docker buildx build --platform linux/arm64 -t kurento-buildpackage:arm64 docker/kurento-buildpackage/
```

### Step 2: Component Compilation
For each architecture, builds components in dependency order:
1. cmake-utils
2. module-creator
3. jsonrpc
4. module-core
5. module-elements
6. module-filters
7. media-server

### Step 3: Package Collection
Debian packages (.deb files) are placed in architecture-specific directories:
```
server/packages/
├── amd64/
│   ├── kurento-cmake-utils_7.3.0_amd64.deb
│   ├── kurento-module-creator_7.3.0_amd64.deb
│   ├── kurento-jsonrpc_7.3.0_amd64.deb
│   ├── kurento-module-core_7.3.0_amd64.deb
│   ├── kurento-module-elements_7.3.0_amd64.deb
│   ├── kurento-module-filters_7.3.0_amd64.deb
│   └── kurento-media-server_7.3.0_amd64.deb
└── arm64/
    ├── kurento-cmake-utils_7.3.0_arm64.deb
    ├── kurento-module-creator_7.3.0_arm64.deb
    ├── kurento-jsonrpc_7.3.0_arm64.deb
    ├── kurento-module-core_7.3.0_arm64.deb
    ├── kurento-module-elements_7.3.0_arm64.deb
    ├── kurento-module-filters_7.3.0_arm64.deb
    └── kurento-media-server_7.3.0_arm64.deb
```

## Environment Variables

### ARCHITECTURES
Override default architectures:
```bash
# Build for amd64 only
ARCHITECTURES="linux/amd64" ./build_all_docker.sh

# Build for arm64 only
ARCHITECTURES="linux/arm64" ./build_all_docker.sh

# Default: both
ARCHITECTURES="linux/amd64,linux/arm64" ./build_all_docker.sh
```

## Troubleshooting

### Docker buildx not available
```bash
docker buildx version
# If not available, install Docker Desktop or Docker with buildx plugin
```

### QEMU not configured
```bash
# Install QEMU on Linux
docker run --privileged --rm tonistiigi/binfmt --install all

# Docker Desktop includes QEMU by default
```

### Build fails for arm64 on amd64 host
- This is expected to be slower due to QEMU emulation
- Ensure sufficient memory (at least 4GB per architecture)
- Consider building on native hardware if available

### Packages missing after build
Check that the build completed successfully:
```bash
ls -lh server/packages/amd64/
ls -lh server/packages/arm64/
```

## Publishing Multi-Arch Images

To publish to a Docker registry:

```bash
# 1. Tag images
docker tag kurento-media-server:local-amd64 myregistry/kurento:7.3.0-amd64
docker tag kurento-media-server:local-arm64 myregistry/kurento:7.3.0-arm64

# 2. Push individual images
docker push myregistry/kurento:7.3.0-amd64
docker push myregistry/kurento:7.3.0-arm64

# 3. Create multi-arch manifest
docker manifest create myregistry/kurento:7.3.0 \
  myregistry/kurento:7.3.0-amd64 \
  myregistry/kurento:7.3.0-arm64

# 4. Push manifest
docker manifest push myregistry/kurento:7.3.0

# 5. (Optional) Tag as latest
docker manifest create myregistry/kurento:latest \
  myregistry/kurento:7.3.0-amd64 \
  myregistry/kurento:7.3.0-arm64
docker manifest push myregistry/kurento:latest
```

## CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Multi-Arch Build

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Set up QEMU
        uses: docker/setup-qemu-action@v2

      - name: Build all architectures
        run: ./build_all_docker.sh

      - name: Create Docker images
        run: ./create_docker_image_from_packages.sh multi

      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: packages
          path: server/packages/
```

## Performance Notes

- **amd64 build on amd64 host**: Native speed
- **arm64 build on amd64 host**: ~5-10x slower due to QEMU emulation
- **arm64 build on arm64 host**: Native speed
- **Parallel builds**: Components build sequentially per architecture, but architectures can build in parallel with sufficient resources

For fastest builds:
- Use native hardware when possible
- Build architectures on separate machines in parallel
- Use CI/CD with matrix builds for parallelization
