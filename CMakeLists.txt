cmake_minimum_required(VERSION 2.8)

project("kms-cmake-utils")
message(STATUS "Running CMake for project: ${PROJECT_NAME}")

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/CMake")

include(KurentoGitHelpers)
get_git_version(PROJECT_VERSION 1.3.3-dev)

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

set(DISABLE_TESTS FALSE CACHE BOOL "Disable tests execution")

if(NOT ${DISABLE_TESTS})
  add_subdirectory(test)
endif()
