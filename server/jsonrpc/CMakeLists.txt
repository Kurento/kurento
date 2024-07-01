cmake_minimum_required(VERSION 3.5)

project("kurento-jsonrpc")
message(STATUS "CMake project: ${PROJECT_NAME}")

include(GNUInstallDirs) # CMAKE_INSTALL_*

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")
enable_testing()

# Add CMake modules provided by this project.
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}") # find_package() search path for `FindKmsJsonRpc.cmake`.

# Export CMake search paths if this is not the top-level project.
get_directory_property(HAS_PARENT PARENT_DIRECTORY)
if(HAS_PARENT)
  set(CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH}" PARENT_SCOPE)
endif()

find_package(KurentoUtils)
if(NOT KurentoUtils_FOUND)
  message(FATAL_ERROR "KurentoUtils not found; please install package 'kurento-cmake-utils'")
endif()

include(KurentoGitHelpers)
install_git_hook(pre-commit ${CMAKE_CURRENT_SOURCE_DIR}/hooks/pre-commit.hook)
get_git_version(PROJECT_VERSION 7.0.2-dev)
message(STATUS "Project version: ${PROJECT_NAME}-${PROJECT_VERSION}")

# Compiler flags
include(CommonBuildFlags)
common_buildflags_set()
#common_buildflags_print()

find_package(PkgConfig)
pkg_check_modules(JSONCPP REQUIRED jsoncpp>=1.7.2)

find_package(Boost REQUIRED COMPONENTS unit_test_framework)

set(CUSTOM_PREFIX "kurento")
set(INCLUDE_PREFIX "${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}/jsonrpc")

if(NOT DEFINED SYSCONF_INSTALL_DIR)
  set(SYSCONF_INSTALL_DIR etc CACHE PATH
      "Install directory for system-wide configuration files")
endif(NOT DEFINED SYSCONF_INSTALL_DIR)

set(prefix ${CMAKE_INSTALL_PREFIX})
set(exec_prefix "\${prefix}")
set(libdir "\${exec_prefix}/${CMAKE_INSTALL_LIBDIR}")
set(includedir "\${prefix}/${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}")
set(requires "jsoncpp")

configure_file(libjsonrpc.pc.in
  ${PROJECT_BINARY_DIR}/libjsonrpc.pc
  @ONLY
)
install(
  FILES ${PROJECT_BINARY_DIR}/libjsonrpc.pc
  DESTINATION ${CMAKE_INSTALL_LIBDIR}/pkgconfig
)

configure_file(FindKmsJsonRpc.cmake.in
  ${PROJECT_BINARY_DIR}/FindKmsJsonRpc.cmake
  @ONLY
)
install(
  FILES ${PROJECT_BINARY_DIR}/FindKmsJsonRpc.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
)

add_subdirectory(src)
add_subdirectory(tests)
