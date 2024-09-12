# Disable error when functions are unused. This allows commenting out calls
# to skip running certain tests during a debug session.
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error=unused-function")

set(TEST_VARIABLES
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}:$ENV{GST_PLUGIN_PATH}"
)
set(VALGRIND_TEST_VARIABLES
  "${TEST_VARIABLES}"
)
list(APPEND SUPPRESSIONS
  "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_test_program(test_constructors constructors.cpp)
if(TARGET ${LIBRARY_NAME}module)
  add_dependencies(test_constructors ${LIBRARY_NAME}module)
endif()
add_dependencies(test_constructors ${LIBRARY_NAME})
set_property(TARGET test_constructors
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../..
    ${JSONRPC_INCLUDE_DIRS}
    ${SIGCPP_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${KMSCORE_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)
target_link_libraries(test_constructors
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
  ${glibmm-2.0_LIBRARIES}
)

add_test_program(test_webrtc_endpoint webRtcEndpoint.cpp)
add_dependencies(test_webrtc_endpoint kmselements)
set_property(TARGET test_webrtc_endpoint
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${KMSCORE_INCLUDE_DIRS}
    ${glibmm-2.4_INCLUDE_DIRS}
    ${gstreamer-1.5_INCLUDE_DIRS}
)
target_link_libraries(test_webrtc_endpoint
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
  ${glibmm-2.4_LIBRARIES}
)

add_test_program(test_rtp_endpoint rtpEndpoint.cpp)
add_dependencies(test_rtp_endpoint kmselements)
set_property(TARGET test_rtp_endpoint
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../..
    ${KMSCORE_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)
target_link_libraries(test_rtp_endpoint
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
)

add_test_program(test_recorder recorderEndpoint.cpp)
add_dependencies(test_recorder kmselements)
set_property(TARGET test_recorder
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${KMSCORE_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)
target_link_libraries(test_recorder
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
)

add_test_program(test_player playerEndpoint.cpp)
add_dependencies(test_player kmselements)
set_property(TARGET test_player
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../..
    ${KMSCORE_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)
target_link_libraries(test_player
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
)
