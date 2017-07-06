set (SOURCE_FILES
  JsonRpcHandler.cpp
  JsonRpcClient.cpp
  JsonRpcUtils.cpp
  JsonFixes.cpp
)

set (HEADER_FILES
  jsonrpc/JsonRpcHandler.hpp
  jsonrpc/JsonRpcClient.hpp
  jsonrpc/JsonRpcException.hpp
  jsonrpc/JsonSerializer.hpp
  jsonrpc/JsonRpcUtils.hpp
  jsonrpc/JsonRpcConstants.hpp
  jsonrpc/JsonFixes.hpp
)

add_library(jsonrpc SHARED ${SOURCE_FILES} ${HEADER_FILES})

set_target_properties(jsonrpc PROPERTIES PUBLIC_HEADER "${HEADER_FILES}")

set_property (TARGET jsonrpc
  PROPERTY INCLUDE_DIRECTORIES
    ${JSONCPP_INCLUDE_DIRS}
    ${Boost_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/jsonrpc
  )

target_link_libraries(jsonrpc
  ${JSONCPP_LIBRARIES}
  ${Boost_LIBRARIES}
)

set_target_properties(jsonrpc PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

install(
  TARGETS jsonrpc
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)
