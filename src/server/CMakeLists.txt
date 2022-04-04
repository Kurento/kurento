include(CodeGenerator)

generate_code(
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  MODULE_EXTRA_INCLUDE_DIRS ${OpenCV_INCLUDE_DIRS}
  SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS ${OpenCV_INCLUDE_DIRS}
  SERVER_IMPL_LIB_EXTRA_LIBRARIES ${OpenCV_LIBRARIES}
  SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects

  # OpenCV search name for pkg-config module file; e.g. in Ubuntu:
  # /usr/lib/x86_64-linux-gnu/pkgconfig/opencv4.pc
  SERVER_IMPL_LIB_PKGCONFIG_EXTRA_REQUIRES "opencv4"

  # OpenCV search name for CMake module file; e.g. in Ubuntu:
  # /usr/lib/x86_64-linux-gnu/cmake/opencv4/OpenCVConfig.cmake
  SERVER_IMPL_LIB_FIND_CMAKE_EXTRA_LIBRARIES "OpenCV"
)
