set (TEST_PROPERTIES
  GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}
)

add_test_program (test_constructors constructors.cpp)
add_dependencies(test_constructors
  ${LIBRARY_NAME}module
  ${LIBRARY_NAME}impl
  facedetector
  faceoverlay
  imageoverlay
  movementdetector
  opencvfilter
)
set_property (TARGET test_constructors
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_BINARY_DIR}
    ${JSONRPC_INCLUDE_DIRS}
    ${SIGCPP_INCLUDE_DIRS}
    ${KMSCORE_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${DEPENDENCIES_INCLUDE_DIRS}
)
target_link_libraries(test_constructors
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
  ${GLIBMM_LIBRARIES}
)
