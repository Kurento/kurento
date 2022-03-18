set(TEST_VARIABLES
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}"
)

add_test_program(test_filters_constructors constructors.cpp)
if(TARGET ${LIBRARY_NAME}module)
  add_dependencies(test_filters_constructors ${LIBRARY_NAME}module)
endif()

# FIXME: Modules disabled because they were written for the C API of OpenCV 2.0
# (Ubuntu 14.04, 16.04), which was maintained for backwards compatibility on
# OpenCV 3.0 (Ubuntu 18.04), but has been finally dropped on OpenCV 4.0 (20.04).
add_dependencies(test_filters_constructors
  #kmsfacedetector
  #kmsfaceoverlay
  #kmsimageoverlay
  #kmsmovementdetector
  kmsopencvfilter
)
set_property(TARGET test_filters_constructors
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../..
    ${JSONRPC_INCLUDE_DIRS}
    ${SIGCPP_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${KMSFILTERS_DEPENDENCIES_INCLUDE_DIRS}
)
target_link_libraries(test_filters_constructors
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
  ${GLIBMM_LIBRARIES}
)
