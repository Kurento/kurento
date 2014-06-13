cmake_minimum_required(VERSION 2.8)

#TODO Fix version getting it from git
set (PACKAGE_VERSION 0.0.1)

configure_file(FindKurentoHelpers.cmake.in
  "${CMAKE_BINARY_DIR}/FindKurentoHelpers.cmake" @ONLY)

set (CMAKE_DIST_FILES
  FindGLIB-GENMARSHAL.cmake
  FindGLIB-MKENUMS.cmake
  GLibHelpers.cmake
  TestHelper.cmake
)

foreach (DIST_FILE ${CMAKE_DIST_FILES})
  install(FILES CMake/${DIST_FILE} DESTINATION share/cmake-2.8/Modules)
endforeach()

install(
  FILES "${CMAKE_BINARY_DIR}/FindKurentoHelpers.cmake"
  DESTINATION share/cmake-2.8/Modules
)
