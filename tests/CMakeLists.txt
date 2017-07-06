include (TestHelpers)

add_test_program (test_rpc TestRpc.cpp)

set_property (TARGET test_rpc
  PROPERTY INCLUDE_DIRECTORIES
    ${JSONCPP_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../src/
)

target_link_libraries(test_rpc
  ${JSONCPP_LINK_LIBRARIES}
  ${JSONRPC_LINK_LIBRARIES}
  jsonrpc
)

add_test_program (test_serialize TestSerialize.cpp)

set_property (TARGET test_serialize
  PROPERTY INCLUDE_DIRECTORIES
    ${JSONCPP_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../src/
)

target_link_libraries(test_serialize
  ${JSONCPP_LINK_LIBRARIES}
  ${JSONRPC_LINK_LIBRARIES}
  jsonrpc
)
