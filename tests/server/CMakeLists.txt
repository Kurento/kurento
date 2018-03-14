set(TEST_VARIABLES
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}"
)

add_test_program(test_filters_constructors constructors.cpp)
if(TARGET ${LIBRARY_NAME}module)
  add_dependencies(test_filters_constructors ${LIBRARY_NAME}module)
endif()
add_dependencies(test_filters_constructors
  facedetector
  faceoverlay
  imageoverlay
  movementdetector
  opencvfilter
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
