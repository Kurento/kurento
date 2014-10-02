cmake_minimum_required(VERSION 2.8)

project(kms-cmake-utils)

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_SOURCE_DIR}/CMake")

set (CMAKE_MODULES_INSTALL_DIR
  share/cmake-2.8/Modules
  CACHE STRING
  "Destination (relative to CMAKE_INSTALL_PREFIX) for cmake modules files"
)

include (KurentoGitHelpers)

get_git_version (PROJECT_VERSION 1.0.1-dev)

configure_file(FindKurentoHelpers.cmake.in
  "${CMAKE_BINARY_DIR}/FindKurentoHelpers.cmake" @ONLY)

set (CMAKE_DIST_FILES
  FindGLIB-GENMARSHAL.cmake
  FindGLIB-MKENUMS.cmake
  GLibHelpers.cmake
  TestHelpers.cmake
  KurentoGitHelpers.cmake
  GenericFind.cmake
  VersionHelpers.cmake
)

foreach (DIST_FILE ${CMAKE_DIST_FILES})
  install(FILES CMake/${DIST_FILE} DESTINATION share/cmake-2.8/Modules)
endforeach()

install(
  FILES "${CMAKE_BINARY_DIR}/FindKurentoHelpers.cmake"
  DESTINATION ${CMAKE_MODULES_INSTALL_DIR}
)
