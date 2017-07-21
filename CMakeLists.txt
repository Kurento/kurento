cmake_minimum_required(VERSION 2.8)

project("kms-cmake-utils")
message(STATUS "CMake project: ${PROJECT_NAME}")

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/CMake")
include(KurentoGitHelpers)
get_git_version(PROJECT_VERSION 1.4.0)
message(STATUS "Project version: ${PROJECT_NAME}-${PROJECT_VERSION}")

set(CMAKE_MODULES_INSTALL_DIR
  share/cmake-${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION}/Modules
  CACHE STRING
  "Destination (relative to CMAKE_INSTALL_PREFIX) for cmake modules files"
)

configure_file(FindKurentoHelpers.cmake.in
  "${CMAKE_CURRENT_BINARY_DIR}/FindKurentoHelpers.cmake" @ONLY)

set(CMAKE_DIST_FILES
  DpkgBuildFlags.cmake
  FindGLIB-GENMARSHAL.cmake
  FindGLIB-MKENUMS.cmake
  GLibHelpers.cmake
  GenericFind.cmake
  KurentoGitHelpers.cmake
  TestHelpers.cmake
  VersionHelpers.cmake
)

foreach(DIST_FILE ${CMAKE_DIST_FILES})
  install(FILES CMake/${DIST_FILE} DESTINATION ${CMAKE_MODULES_INSTALL_DIR})
endforeach()

install(
  FILES "${CMAKE_CURRENT_BINARY_DIR}/FindKurentoHelpers.cmake"
  DESTINATION ${CMAKE_MODULES_INSTALL_DIR}
)

if(NOT ${DISABLE_TESTS})
  add_subdirectory(test)
endif()
