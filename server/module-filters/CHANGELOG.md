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

### Added
- Logging: Add support for libsoup debug logging
- GStreamerFilters: Add setElementProperty() for real time updating of properties. Supported types:
  - Int
  - Float
  - Double
  - Enum

### Changed
- API: Review documentation, update links

### Fixed
- clang-tidy fixes

## [6.7.1] - 2018-03-21

### Changed
- Push version to 6.7.1.

## [6.7.0] - 2018-01-24

### Changed
- CMake: Compile and link as Position Independent Code (`-fPIC`).
- Add more verbose logging in some areas that required it.
- Debian: Align all version numbers of KMS-related modules.
- Debian: Remove version numbers from package names.
- Debian: Configure builds to use parallel compilation jobs.

## [6.6.2] - 2017-07-24

### Changed
- Old ChangeLog.md moved to the new format in this CHANGELOG.md file.
- CMake: Full review of all CMakeLists.txt files to tidy up and homogenize code style and compiler flags.
- CMake: Position Independent Code flags (`-fPIC`) were scattered around projects, and are now removed. Instead, the more CMake-idiomatic variable "CMAKE_POSITION_INDEPENDENT_CODE" is used.
- CMake: All projects now compile with `-std=c11|-std=c++11 -Wall -Werror -pthread`.
- CMake: Debug builds now compile with `-g -O0` (while default CMake used `-O1` for Debug builds).
- CMake: include() and import() commands were moved to the code areas where they are actually required.

## [6.6.1] - 2016-09-30

### Changed
- Improve compilation process.
- CMake: Rename library testutils to filtertestutils.
- CMake: Rename constructor test to filters_constructors.
- CMake: Avoid using global cmake directories.
- CMake: Avoid the use of global include directories.

## [6.6.0] - 2016-09-09

## [6.5.0] - 2016-05-27

### Changed
- Changed license to Apache 2.0.
- Updated documentation.
- Test: Update and activate filter tests.
- Logooverlay: Add test for kmslogooverlay filter.

## [6.4.0] - 2016-02-24

## [6.3.0] - 2019-01-19

## 6.2.0 - 2015-11-25

### Added
- OpenCvFilter: Now exceptions raised in OpenCV code are sent to the client as errors.

### Changed
- Update GStreamer version to 1.7.
- GStreamerFilter: Improve command parser using `gst-launch` parser.
- KmsOpencvFilter: Convert KurentoExceptions into bus messages.

[6.8.0]: https://github.com/Kurento/kms-filters/compare/6.7.1...6.8.0
[6.7.1]: https://github.com/Kurento/kms-filters/compare/6.7.0...6.7.1
[6.7.0]: https://github.com/Kurento/kms-filters/compare/6.6.2...6.7.0
[6.6.2]: https://github.com/Kurento/kms-filters/compare/6.6.1...6.6.2
[6.6.1]: https://github.com/Kurento/kms-filters/compare/6.6.0...6.6.1
[6.6.0]: https://github.com/Kurento/kms-filters/compare/6.5.0...6.6.0
[6.5.0]: https://github.com/Kurento/kms-filters/compare/6.4.0...6.5.0
[6.4.0]: https://github.com/Kurento/kms-filters/compare/6.3.0...6.4.0
[6.3.0]: https://github.com/Kurento/kms-filters/compare/6.2.0...6.3.0
