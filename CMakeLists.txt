cmake_minimum_required(VERSION 2.8)

set (CMAKE_DIST_FILES
  FindGLIB-GENMARSHAL.cmake
  FindGLIB-MKENUMS.cmake
  GLibHelpers.cmake
  TestHelper.cmake
)

foreach (DIST_FILE ${CMAKE_DIST_FILES})
  install(FILES CMake/${DIST_FILE} DESTINATION share/cmake-2.8/Modules)
endforeach()
