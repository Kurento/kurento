# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 6.10.0 - 2019-04-03

Starting from 6.10.0, all of these Kurento Media Server sub-projects

- kurento-module-creator
- kms-cmake-utils
- kms-core
- kms-elements
- kms-filters
- kms-jsonrpc
- kurento-media-server

have their ChangeLogs unified in [kurento-media-server/CHANGELOG](https://github.com/Kurento/kurento-media-server/blob/master/CHANGELOG.md).


## [6.8.0] - 2018-09-26

### Fixed
- clang-tidy fixes

## [6.7.0] - 2018-01-24

### Changed
- CMake: Compile and link as Position Independent Code ('-fPIC').
- Debian: Align all version numbers of KMS-related modules.
- Debian: Remove version numbers from package names.
- Debian: Configure builds to use parallel compilation jobs.

## [1.1.3] - 2017-07-24

### Changed
- Old ChangeLog.md moved to the new format in this CHANGELOG.md file.
- CMake: Full review of all CMakeLists.txt files to tidy up and homogenize code style and compiler flags.
- CMake: Position Independent Code flags ("-fPIC") were scattered around projects, and are now removed. Instead, the more CMake-idiomatic variable "CMAKE_POSITION_INDEPENDENT_CODE" is used.
- CMake: All projects now compile with "[-std=c11|-std=c++11] -Wall -Werror -pthread".
- CMake: Debug builds now compile with "-g -O0" (while default CMake used "-O1" for Debug builds).
- CMake: include() and import() commands were moved to the code areas where they are actually required.

### Fixed
- Fix warning "used uninitialized" (`-Werror=maybe-uninitialized`).

## [1.1.2] - 2016-09-30

### Changed
- FindKmsJsonRpc: Allow importing project locally.
- CMake: Do not add includes globally.
- TestSerialize: initialize variables properly.

## [1.1.1] - 2016-05-27

### Changed
- Changed license to Apache 2.0.
- Updated documentation.

## [1.1.0] - 2019-01-19

### Added
- JsonRpcUtils: Add method to get a boolean parameter.
- Update version to 1.1.0 due to changes in API.

## 1.0.1 - 2015-11-25

### Changed
- Updated README.md according to FIWARE guidelines.
- Added links to readthedocs.org and apiary.io in README.

[6.8.0]: https://github.com/Kurento/kms-jsonrpc/compare/6.7.0...6.8.0
[6.7.0]: https://github.com/Kurento/kms-jsonrpc/compare/1.1.3...6.7.0
[1.1.3]: https://github.com/Kurento/kms-jsonrpc/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/Kurento/kms-jsonrpc/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/Kurento/kms-jsonrpc/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/Kurento/kms-jsonrpc/compare/1.0.1...1.1.0
