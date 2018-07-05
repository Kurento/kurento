set(TEST_VARIABLES
  "GST_PLUGIN_PATH=$ENV{GST_PLUGIN_PATH}:${CMAKE_BINARY_DIR}"
)
set(VALGRIND_TEST_VARIABLES
  "${TEST_VARIABLES}"
  "DEBUG_MEDIASET=TRUE"
)
list(APPEND SUPPRESSIONS
  "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_test_program(test_constructors constructors.cpp)
if(TARGET ${LIBRARY_NAME}module)
  add_dependencies(test_constructors ${LIBRARY_NAME}module)
endif()
add_dependencies(test_constructors ${LIBRARY_NAME}plugins)
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
    ${gstreamer-1.5_INCLUDE_DIRS}
)
target_link_libraries(test_constructors
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
  ${glibmm-2.0_LIBRARIES}
)

# add_test_program(test_webrtc_endpoint webRtcEndpoint.cpp)
# add_dependencies(test_webrtc_endpoint kmselementsplugins ${LIBRARY_NAME}impl)
# set_property(TARGET test_webrtc_endpoint
#   PROPERTY INCLUDE_DIRECTORIES
#     ${KmsJsonRpc_INCLUDE_DIRS}
#     ${sigc++-2.0_INCLUDE_DIRS}
#     ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
#     ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
#     ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
#     ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
#     ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
#     ${KMSCORE_INCLUDE_DIRS}
#     ${glibmm-2.4_INCLUDE_DIRS}
#     ${gstreamer-1.5_INCLUDE_DIRS}
# )
# target_link_libraries(test_webrtc_endpoint
#   ${LIBRARY_NAME}impl
#   ${KMSCORE_LIBRARIES}
#   ${glibmm-2.4_LIBRARIES}
# )

add_test_program(test_rtp_endpoint rtpEndpoint.cpp)
add_dependencies(test_rtp_endpoint kmselementsplugins)
set_property(TARGET test_rtp_endpoint
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${KMSCORE_INCLUDE_DIRS}
    ${gstreamer-1.5_INCLUDE_DIRS}
)
target_link_libraries(test_rtp_endpoint
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
)

add_test_program(test_recorder recorderEndpoint.cpp)
add_dependencies(test_recorder kmselementsplugins)
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
    ${gstreamer-1.5_INCLUDE_DIRS}
)
target_link_libraries(test_recorder
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
)

add_test_program(test_player playerEndpoint.cpp)
add_dependencies(test_player kmselementsplugins)
set_property(TARGET test_player
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${KMSCORE_INCLUDE_DIRS}
    ${gstreamer-1.5_INCLUDE_DIRS}
)
target_link_libraries(test_player
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
)
